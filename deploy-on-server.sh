#!/usr/bin/env bash
# ============================================================
#  xzs 智慧乐学系统 —— ECS 服务器端一键部署
#  用法（在 FinalShell 终端 / 阿里云 Workbench 里执行）：
#      bash /root/deploy-on-server.sh
#  前置：已把 ecs-deploy-20260903.tar.gz 上传到 /root/ 目录
#
#  流程：公钥注入 → 环境体检 → 开 swap → 解包 → 冲突清理
#        → 逐个构建镜像 → 启动 → 健康检查 → 数据库校验
# ============================================================
set -uo pipefail

TAR="/root/ecs-deploy-20260903.tar.gz"
DIR="/root/xzs-deploy"
# 注入两把公钥：① 本机主密钥 ② 专用于自动化推送的免密部署密钥
PUBKEYS=(
  "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJ0p0HPrAOPzbOintNVYfEnOzD0Rrrq94sEuru0O3Spc"
  "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQD6LzswEqYlBTxEsmmbI5Wfz5xkKzORuPQQ7/3JCfHZ6ZtcFwJmPszVuzBSveTH5zR4q9jEQCulTf8d+86O4JUPV4ep43IldCavVCJtBlgEfATUn6RcFng1DegAnHMbWxwtFgDLE+cZxaUPfpqbf4iylxYQHl6z65CFbqTkXJAq+pHuZaVlPCR22JneOMFXCyWqdLB6espAm3Im9yLS/rR+VTtSrhVq6gQ8VNfHPcPUtP1ahNJmNUO/7vfOcjEctF3yXnPNpKYI5feIwDe2M1hukLb/norujzDfTxx7PV0OsR+3KDFjJHqDBZqrMuoykK7tM15kxUn1w6t1pqCi58qpy7UlHdfNq0XQfm8kweg8KJ0MBjmzRooFRoapadnUAvDvJoO6c6S1O7jOxQwQDVQXI2KCYDJNOh2k/KAdGDCJg7cQGvJ6hVXxpSGINpEkQaP/8j/HLrLAoBUoHEOA3219IwOiFoY7LzkMqXooS76TLGBsvAs3gO8DAUGOfCcupsM= xzs-ecs-deploy"
)

log()  { echo -e "\033[32m[DEPLOY]\033[0m $*"; }
warn() { echo -e "\033[33m[WARN ]\033[0m $*"; }
err()  { echo -e "\033[31m[ERROR]\033[0m $*"; exit 1; }

# ============================================================
# 0. 基础校验
# ============================================================
[ "$(id -u)" -ne 0 ] && err "请用 root 执行（或 sudo -i 后重试）"

# 脚本自身所在目录：若已随包解压到 $DIR，则无需再解包
SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALREADY_EXTRACTED=0
if [ "$SELF_DIR" = "$DIR" ] && [ -f "$DIR/docker-compose.yml" ]; then
  ALREADY_EXTRACTED=1
fi
if [ "$ALREADY_EXTRACTED" != "1" ] && [ ! -f "$TAR" ]; then
  err "找不到 $TAR —— 请先用 FinalShell 把该压缩包上传到 /root/ 目录"
fi
echo

# ============================================================
# 1. 注入免密公钥（注入后本机即可用脚本直接推送后续更新）
# ============================================================
log "==> [1/9] 注入免密公钥"
mkdir -p ~/.ssh && chmod 700 ~/.ssh
touch ~/.ssh/authorized_keys
for pk in "${PUBKEYS[@]}"; do
  FP=$(echo "$pk" | awk '{print $2}')
  if grep -qF "$FP" ~/.ssh/authorized_keys 2>/dev/null; then
    echo "    已存在，跳过: ${FP:0:40}..."
  else
    echo "$pk" >> ~/.ssh/authorized_keys
    echo "    已注入: ${FP:0:40}..."
  fi
done
chmod 600 ~/.ssh/authorized_keys
# 确保 sshd 允许公钥登录（有些镜像默认被关掉）
if [ -f /etc/ssh/sshd_config ]; then
  sed -i -E 's/^#?\s*PubkeyAuthentication\s+.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config
  grep -q '^PubkeyAuthentication yes' /etc/ssh/sshd_config || echo 'PubkeyAuthentication yes' >> /etc/ssh/sshd_config
  (systemctl restart sshd || service sshd restart || true) >/dev/null 2>&1
fi
log "    公钥已注入，后续可免密推送"
echo

# ============================================================
# 2. 环境体检
# ============================================================
log "==> [2/9] 环境体检"
docker --version 2>/dev/null || err "Docker 未安装 —— 请先运行 ecs_bootstrap.sh 安装 Docker"
DC="docker compose"
docker compose version >/dev/null 2>&1 || { DC="docker-compose"; docker-compose --version >/dev/null 2>&1 || err "docker compose 未安装"; }
echo "    compose 命令: $DC"

TOTAL_MEM=$(free -m | awk '/^Mem:/{print $2}')
DISK_AVAIL=$(df -BG / | awk 'NR==2{gsub("G","",$4); print $4}')
echo "    内存: ${TOTAL_MEM} MB    磁盘可用: ${DISK_AVAIL} GB    内核: $(uname -r)"
[ "$DISK_AVAIL" -lt 15 ] && warn "磁盘可用不足 15GB，构建镜像（含 Maven/Node 中间层）可能失败，建议清理后再来"
[ "$TOTAL_MEM" -lt 3500 ] && warn "内存 ${TOTAL_MEM}MB 偏小，将启用 swap 兜底"
echo

# ============================================================
# 3. 开启 swap（2C2G 构建 Vue + Maven 必备，否则极易 OOM）
# ============================================================
log "==> [3/9] 检查/配置 swap"
SWAP_MB=$(free -m | awk '/^Swap:/{print $2}')
if [ "${SWAP_MB:-0}" -ge 3000 ]; then
  log "    swap 已有 ${SWAP_MB} MB，跳过"
else
  if [ ! -f /swapfile ]; then
    log "    创建 4GB swapfile（约需 1-2 分钟）..."
    dd if=/dev/zero of=/swapfile bs=1M count=4096 status=progress 2>/dev/null || \
      dd if=/dev/zero of=/swapfile bs=1M count=4096 2>/dev/null
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null 2>&1
  fi
  swapon /swapfile 2>/dev/null || warn "swapon 失败（多为容器化内核限制），构建可能吃紧"
  grep -qF '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  sysctl -w vm.swappiness=60 >/dev/null 2>&1 || true
  log "    swap 现在: $(free -m | awk '/^Swap:/{print $2}') MB"
fi
echo

# ============================================================
# 4. 解包
# ============================================================
log "==> [4/9] 解压部署包 → $DIR"
if [ "$ALREADY_EXTRACTED" = "1" ]; then
  log "    脚本已随包位于 $DIR，跳过解压"
else
  mkdir -p "$DIR"
  tar -xzf "$TAR" -C "$DIR" --strip-components=1
fi
[ -f "$DIR/docker-compose.yml" ] || err "解包失败：没找到 $DIR/docker-compose.yml"
[ -f "$DIR/.env" ] || warn "缺少 .env，将使用 compose 内置默认值"
# 中文文件名/换行保护
cd "$DIR"
set +u
DBNAME=$(grep -E '^MYSQL_DATABASE=' .env 2>/dev/null | cut -d= -f2)
ROOTPWD=$(grep -E '^MYSQL_ROOT_PASSWORD=' .env 2>/dev/null | cut -d= -f2)
set -u
DBNAME=${DBNAME:-xzs}
ROOTPWD=${ROOTPWD:-123456}
log "    目标数据库: ${DBNAME}"
echo

# ============================================================
# 5. 冲突清理（关键：之前手工 docker run --name mysql 会撞名）
# ============================================================
log "==> [5/9] 清理冲突容器与端口"
echo "    当前容器："
docker ps -a --format '      {{.Names}}\t{{.Status}}\t{{.Ports}}'

# 5.1 旧 compose 项目先停
$DC down --remove-orphans >/dev/null 2>&1 || true

# 5.2 手工创建的同名容器：改名保留数据，不删除
for n in mysql backend frontend multi-agent; do
  if docker ps -a --format '{{.Names}}' | grep -qx "$n"; then
    warn "发现遗留容器 '$n'（非 compose 管理），改名为 ${n}-legacy 并停止（数据保留）"
    docker stop "$n" >/dev/null 2>&1 || true
    docker rename "$n" "${n}-legacy" >/dev/null 2>&1 || true
  fi
done

# 5.3 端口占用检查
for p in 8001 8006 8100 13306; do
  PID=$(ss -lntp 2>/dev/null | grep -E ":$p\s" | grep -oP 'pid=\K[0-9]+' | head -1)
  if [ -n "${PID:-}" ]; then
    PNAME=$(ps -p "$PID" -o comm= 2>/dev/null)
    if [ "$PNAME" = "docker-proxy" ]; then
      warn "端口 $p 被 docker-proxy 占用，尝试停掉对应容器"
      CID=$(docker ps --format '{{.ID}} {{.Ports}}' | grep ":$p->" | awk '{print $1}' | head -1)
      [ -n "${CID:-}" ] && docker stop "$CID" >/dev/null 2>&1 || true
    else
      warn "端口 $p 被进程 ${PNAME}(PID ${PID}) 占用，compose 启动会失败，请手动处理"
    fi
  fi
done
log "    冲突清理完成"
echo

# ============================================================
# 6. 逐个构建镜像（串行，降低峰值内存）
# ============================================================
log "==> [6/9] 构建镜像（首次较慢，约 10-25 分钟，请耐心）"

build_one() {
  local svc="$1"
  log "    ---- 构建 $svc ----"
  if $DC build "$svc" 2>&1 | tail -25; then
    log "    $svc 构建完成"
  else
    err "$svc 构建失败 —— 上面是最后 25 行日志。常见原因：磁盘不足 / 内存 OOM / 网络拉不到镜像源"
  fi
}

build_one multi-agent
build_one backend
build_one frontend
echo

# ============================================================
# 7. 启动
# ============================================================
log "==> [7/9] 启动服务"
$DC up -d
sleep 5
$DC ps
echo

# ============================================================
# 8. 健康检查
# ============================================================
log "==> [8/9] 等待服务就绪（MySQL 初始化 + Spring Boot 启动约 1-3 分钟）"
OK=0
for i in $(seq 1 40); do
  sleep 10
  C8006=$(curl -s -o /dev/null -w "%{http_code}" --max-time 4 http://127.0.0.1:8006/ 2>/dev/null || echo 000)
  C8001=$(curl -s -o /dev/null -w "%{http_code}" --max-time 4 http://127.0.0.1:8001/api/admin/education/list 2>/dev/null || echo 000)
  C8100=$(curl -s -o /dev/null -w "%{http_code}" --max-time 4 http://127.0.0.1:8100/api/v1/health 2>/dev/null || echo 000)
  echo "    第 ${i} 次   前端8006=${C8006}   后端8001=${C8001}   多智能体8100=${C8100}"
  if [ "$C8006" != "000" ] && [ "$C8001" != "000" ] && [ "$C8100" != "000" ]; then
    OK=1
    break
  fi
done
echo

# ============================================================
# 9. 数据库校验 + 结果输出
# ============================================================
log "==> [9/9] 数据库校验"
sleep 8
TBL=$($DC exec -T mysql mysql -uroot -p"${ROOTPWD}" -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DBNAME}';" 2>/dev/null | tr -d '\r')
echo "    数据库 ${DBNAME} 表数量: ${TBL:-无法读取}"
if [ -z "${TBL:-}" ] || [ "${TBL:-0}" -lt 20 ]; then
  warn "表数量异常（期望 ≥23），可能是初始化脚本未执行。查看 mysql 日志："
  $DC logs --tail=30 mysql
else
  log "    表结构正常"
fi
echo

log "==================== 容器状态 ===================="
$DC ps
echo
IP=$(curl -s --max-time 3 http://100.100.100.200/latest/meta-data/eip-address 2>/dev/null || true)
[ -z "$IP" ] && IP=$(hostname -I | awk '{print $1}')

if [ "$OK" = "1" ]; then
  log "================================================================"
  log "  部署成功！"
  log "  管理端：   http://${IP}:8006      账号 admin / 123456"
  log "  后端 API： http://${IP}:8001"
  log "  多智能体： http://${IP}:8100/docs"
  log "----------------------------------------------------------------"
  log "  记得在阿里云安全组放行 8006 / 8001 / 8100（如需外部访问 13306 再单独放行）"
  log "  看日志：   cd $DIR && docker compose logs -f backend"
  log "================================================================"
else
  warn "================================================================"
  warn "  部分服务未就绪，以下是排障信息："
  warn "================================================================"
  for s in mysql multi-agent backend frontend; do
    echo "----- $s 最近 20 行日志 -----"
    $DC logs --tail=20 "$s" 2>/dev/null
    echo
  done
fi
