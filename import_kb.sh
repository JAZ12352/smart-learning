#!/bin/bash
# 知识库素材自动导入脚本
# 用法: bash import_kb.sh

BASE="http://localhost:8001/api/admin"
MYSQL="C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe"
CNF="C:\Users\20873\.my.cnf"
MATERIAL="C:\Users\20873\Downloads\zhuhuibeike - 副本\知识库素材"

# 1. 获取 admin 用户的 token（从数据库直接取）
TOKEN=$("$MYSQL" --defaults-extra-file="$CNF" -N -e "SELECT token FROM t_user_token WHERE user_name='admin' ORDER BY id DESC LIMIT 1;" xsz_)
if [ -z "$TOKEN" ]; then
  echo "无法获取 token，请先登录一次"
  exit 1
fi
echo "Token: ${TOKEN:0:20}..."

# 2. 创建知识库
KB_ID=$(curl -s -X POST "$BASE/knowledge-base/create" \
  -H "Content-Type: application/json" \
  -H "token: $TOKEN" \
  -d '{"name":"中小学教育资源库","description":"覆盖小初高三学段、多学科的结构化教学知识库，支持 RAG 检索增强生成","subject":"multiple"}' | \
  grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "知识库ID: $KB_ID"

# 3. 批量上传素材
if [ -n "$KB_ID" ]; then
  for f in $(find "$MATERIAL" -name "*.txt" -type f); do
    echo "上传: $(basename "$f")"
    curl -s -X POST "$BASE/knowledge-base/upload" \
      -H "token: $TOKEN" \
      -F "file=@$f" \
      -F "kbId=$KB_ID"
    echo ""
  done
  echo ""
  echo "=== 导入完成 ==="
  echo "知识库ID: $KB_ID"
else
  echo "知识库创建失败，可能已存在。尝试获取已有知识库列表..."
  curl -s "$BASE/knowledge-base/list" -H "token: $TOKEN"
fi
