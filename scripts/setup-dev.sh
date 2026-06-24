#!/bin/bash
set -e

echo "=== DocChat 开发环境初始化 ==="

# 1. 构建后端
echo "[1/4] 构建后端..."
cd server && mvn clean package -DskipTests && cd ..

# 2. 安装前端依赖
echo "[2/4] 安装前端依赖..."
cd web && pnpm install && cd ..

# 3. 构建聊天组件
echo "[3/4] 构建聊天组件..."
cd packages/chat-widget && pnpm install && pnpm build && cd ../..

# 4. 启动 Docker Compose
echo "[4/4] 启动服务..."
docker compose -f docker-compose.yml up -d

echo "=== 开发环境启动完成 ==="
echo "前端: http://localhost"
echo "后端: http://localhost:8080"
echo "Milvus: http://localhost:19530"
