#!/usr/bin/env bash
# 知识库素材批量导入
#
# 用法：
#   MATERIAL=./知识库素材 bash scripts/import_kb.sh
#
# 环境变量（均可选）：
#   BASE        后端接口地址      默认 http://localhost:8001/api/admin
#   MYSQL       mysql 客户端命令  默认 mysql
#   MYSQL_CNF   数据库连接配置文件 默认 ~/.my.cnf（需含 user/password/host）
#   DB          数据库名          默认 xzs
#   MATERIAL    素材目录（.txt）   默认 ./知识库素材
set -euo pipefail

BASE="${BASE:-http://localhost:8001/api/admin}"
MYSQL="${MYSQL:-mysql}"
CNF="${MYSQL_CNF:-$HOME/.my.cnf}"
DB="${DB:-xzs}"
MATERIAL="${MATERIAL:-./知识库素材}"

if [ ! -d "$MATERIAL" ]; then
  echo "素材目录不存在：$MATERIAL" >&2
  echo "请用 MATERIAL=<目录> 指定，或把素材放到 ./知识库素材" >&2
  exit 1
fi

# 1. 从数据库取 admin 的 token（需先在后台登录一次，产生 t_user_token 记录）
TOKEN="$("$MYSQL" --defaults-extra-file="$CNF" -N -e \
  "SELECT token FROM t_user_token WHERE user_name='admin' ORDER BY id DESC LIMIT 1;" "$DB")"
if [ -z "$TOKEN" ]; then
  echo "无法获取 token，请先在管理端登录一次" >&2
  exit 1
fi
echo "Token: ${TOKEN:0:20}..."

# 2. 创建知识库
KB_ID="$(curl -s -X POST "$BASE/knowledge-base/create" \
  -H "Content-Type: application/json" \
  -H "token: $TOKEN" \
  -d '{"name":"领域知识库","description":"面向垂直领域技能培训的结构化知识库，支持 RAG 检索增强生成","subject":"multiple"}' \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)"
echo "知识库ID: $KB_ID"

# 3. 批量上传素材
if [ -n "$KB_ID" ]; then
  find "$MATERIAL" -name "*.txt" -type f | while read -r f; do
    echo "上传: $(basename "$f")"
    curl -s -X POST "$BASE/knowledge-base/upload" \
      -H "token: $TOKEN" \
      -F "file=@$f" \
      -F "kbId=$KB_ID"
    echo ""
  done
  echo "=== 导入完成，知识库ID: $KB_ID ==="
else
  echo "知识库创建失败，可能已存在。尝试列出已有知识库..."
  curl -s "$BASE/knowledge-base/list" -H "token: $TOKEN"
fi
