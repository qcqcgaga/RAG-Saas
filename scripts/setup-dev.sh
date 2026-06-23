#!/bin/bash
# DocChat 开发环境初始化脚本

set -e

echo "=== DocChat 开发环境初始化 ==="

# 检查依赖
echo "[1/4] 检查依赖..."
command -v java >/dev/null 2>&1 || { echo "❌ Java 21+ 未安装"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven 未安装"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ Node.js 未安装"; exit 1; }
command -v pnpm >/dev/null 2>&1 || { echo "❌ pnpm 未安装: npm install -g pnpm"; exit 1; }
echo "✅ 依赖检查通过"

# 启动基础设施
echo "[2/4] 启动基础设施（PostgreSQL + Redis + Milvus）..."
cd "$(dirname "$0")/../docker"
docker compose up -d postgres redis etcd milvus
echo "⏳ 等待服务就绪..."
sleep 10
echo "✅ 基础设施已启动"

# 安装前端依赖
echo "[3/4] 安装前端依赖..."
cd "$(dirname "$0")/../web"
pnpm install
echo "✅ 前端依赖已安装"

# 安装聊天组件依赖
echo "[4/4] 安装聊天组件依赖..."
cd "$(dirname "$0")/../packages/chat-widget"
pnpm install
echo "✅ 聊天组件依赖已安装"

echo ""
echo "=== 初始化完成 ==="
echo ""
echo "启动后端： cd server && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "启动前端： cd web && pnpm dev"
echo "管理后台： http://localhost:5173"
echo "后端 API： http://localhost:8080"
