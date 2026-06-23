# 部署指南

## 环境要求

- Docker 24+
- Docker Compose 2.x+
- Java 21+（本地开发）
- Node.js 22+、pnpm 9+（本地开发）

## 快速启动（Docker Compose）

```bash
# 1. 设置环境变量
export XUNFEI_API_KEY=your_api_key

# 2. 启动所有服务
cd docker
docker compose up -d

# 3. 查看服务状态
docker compose ps

# 4. 访问
# 管理后台：http://localhost:3000
# 后端 API：http://localhost:8080
```

## 本地开发

### 后端

```bash
cd server
# 需要 PostgreSQL + Redis + Milvus 本地运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端

```bash
cd web
pnpm install
pnpm dev
```

### 聊天组件

```bash
cd packages/chat-widget
pnpm install
pnpm dev
```

## 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| DATABASE_URL | PostgreSQL 连接串 | jdbc:postgresql://localhost:5432/docchat |
| DATABASE_USERNAME | 数据库用户名 | docchat |
| DATABASE_PASSWORD | 数据库密码 | *** |
| REDIS_HOST | Redis 主机 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| MILVUS_HOST | Milvus 主机 | localhost |
| MILVUS_PORT | Milvus 端口 | 19530 |
| XUNFEI_API_KEY | 讯飞 API Key | *** |
