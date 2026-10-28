# ===========================================
# 智能教师管理系统 - 管理端前端 Docker 镜像
# 构建: docker build -t xzs-admin:3.9.0 .
# ===========================================

# ---- Stage 1: Node 构建 ----
FROM node:16-alpine AS builder
WORKDIR /build
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

# ---- Stage 2: Nginx 部署 ----
FROM nginx:1.25-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/ || exit 1
