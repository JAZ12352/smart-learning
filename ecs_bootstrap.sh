#!/usr/bin/env bash
# ============================================================
# 阿里云 ECS 一键引导：安装 Docker + Compose + 配置国内镜像加速
# 适用：Ubuntu/Debian、CentOS/RHEL/Alibaba Cloud Linux、Rocky/Alma
# 用法：sudo bash ecs_bootstrap.sh [可选: --with-swap 内存<4G时创建2G交换分区]
# ============================================================
set -euo pipefail

WITH_SWAP=0
[ "${1:-}" = "--with-swap" ] && WITH_SWAP=1

log() { echo -e "\033[32m[BOOTSTRAP]\033[0m $*"; }
warn() { echo -e "\033[33m[WARN]\033[0m $*"; }

[ "$(id -u)" -ne 0 ] && { echo "请用 root 执行（sudo bash $0）"; exit 1; }

# ---------- 1. 识别系统 ----------
. /etc/os-release
OS_ID="$ID"
log "检测到系统：$PRETTY_NAME"

# ---------- 2. 内存体检 ----------
MEM_MB=$(free -m | awk '/^Mem:/{print $2}')
log "物理内存：${MEM_MB} MB"
if [ "$MEM_MB" -lt 3500 ]; then
  warn "内存低于 4GB：建议 4GB 以上。当前配置下 MySQL+Java(1G)+Python+Nginx 会比较紧张。"
  if [ "$WITH_SWAP" -eq 1 ]; then
    if [ ! -f /swapfile ]; then
      log "创建 2GB 交换分区..."
      fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
      chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
      grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
      log "交换分区已启用：$(free -m | awk '/^Swap:/{print $2}') MB"
    fi
  else
    warn "需要交换分区请重跑：sudo bash $0 --with-swap"
  fi
fi

# ---------- 3. 安装 Docker ----------
if command -v docker >/dev/null 2>&1; then
  log "Docker 已安装：$(docker --version)"
else
  log "安装 Docker..."
  case "$OS_ID" in
    ubuntu|debian)
      apt-get update -y
      apt-get install -y ca-certificates curl gnupg lsb-release
      install -m 0755 -d /etc/apt/keyrings
      curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/"$OS_ID"/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
      chmod a+r /etc/apt/keyrings/docker.gpg
      echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/$OS_ID $(lsb_release -cs) stable" \
        > /etc/apt/sources.list.d/docker.list
      apt-get update -y
      apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      ;;
    centos|rhel|rocky|almalinux|alinux|anolis)
      yum install -y yum-utils device-mapper-persistent-data lvm2
      yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
      yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      ;;
    *)
      warn "未识别的系统（$OS_ID），改用官方安装脚本"
      curl -fsSL https://get.docker.com | bash
      ;;
  esac
fi

# ---------- 4. 配置镜像加速 ----------
mkdir -p /etc/docker
MIRRORS=(
  "https://docker.m.daocloud.io"
  "https://docker.1panel.live"
  "https://hub.rat.dev"
  "https://docker.registry.cyou"
)
log "测试可用的镜像加速器（国内 ECS 直连 Docker Hub 常超时）..."
WORKING=""
for m in "${MIRRORS[@]}"; do
  cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["$m"],
  "log-driver": "json-file",
  "log-opts": {"max-size": "50m", "max-file": "3"}
}
EOF
  systemctl restart docker >/dev/null 2>&1 || true
  sleep 3
  if timeout 90 docker pull nginx:1.25-alpine >/dev/null 2>&1; then
    WORKING="$m"
    log "可用加速器：$m"
    break
  else
    warn "不可用：$m"
  fi
done

if [ -z "$WORKING" ]; then
  warn "所有公共加速器均失败。建议到阿里云容器镜像服务控制台获取「个人加速器地址」，"
  warn "然后写入 /etc/docker/daemon.json 的 registry-mirrors 并执行 systemctl restart docker"
  cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {"max-size": "50m", "max-file": "3"}
}
EOF
  systemctl restart docker >/dev/null 2>&1 || true
fi

# ---------- 5. 开机自启 ----------
systemctl enable docker >/dev/null 2>&1 || true
systemctl start docker

# ---------- 6. 结果 ----------
echo
log "================ 环境就绪 ================"
log "Docker    : $(docker --version)"
log "Compose   : $(docker compose version 2>/dev/null || echo '未安装 docker-compose-plugin')"
log "镜像加速  : ${WORKING:-未配置（需手动处理）}"
log "内存      : ${MEM_MB} MB"
log "磁盘可用  : $(df -h / | awk 'NR==2{print $4}')"
log "=========================================="
