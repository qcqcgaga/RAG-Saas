# 部署方案

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0
> 日期：2026-06-24

## 1. 部署架构

```
┌─────────────────────────────────────────────┐
│              Docker Compose                  │
│                                             │
│  ┌──────┐  ┌──────┐  ┌──────────────────┐  │
│  │  PG  │  │Redis │  │  Milvus          │  │
│  │ :5432│  │ :6379│  │  :19530         │  │
│  └──┬───┘  └──┬───┘  │  (etcd+MinIO)  │  │
│     │         │      └────────┬─────────┘  │
│     │         │               │            │
│  ┌──┴─────────┴───────────────┴──────────┐ │
│  │         Spring Boot Server            │ │
│  │              :8080                    │ │
│  └────────────────┬─────────────────────┘ │
│                   │                        │
│  ┌────────────────┴─────────────────────┐ │
│  │         Nginx (Vue 3)                │ │
│  │              :80                     │ │
│  └──────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

## 2. 部署前置条件

| 条件 | 要求 | 验证命令 |
|------|------|---------|
| Docker Desktop | 已安装并运行 | `docker info` |
| Docker Compose | v2+ | `docker compose version` |
| 磁盘空间 | >= 5GB | `df -h` |
| 内存 | >= 4GB | 系统信息 |
| 端口占用 | 80, 8080, 5432, 6379, 19530 可用 | `netstat -tlnp` |

## 3. 部署步骤

### 步骤 1：构建后端 JAR

```bash
cd server
mvn clean package -DskipTests
```

预期产出：`server/target/docchat-server-0.1.0-SNAPSHOT.jar`

### 步骤 2：启动基础设施

```bash
cd <项目根目录>
docker compose up -d postgres redis etcd minio milvus
```

等待基础设施健康检查通过（约 60-90 秒）：

```bash
docker compose ps  # 确认所有基础设施状态为 healthy
```

### 步骤 3：启动后端服务

```bash
docker compose up -d server
```

等待 Spring Boot 启动完成（约 30 秒）：

```bash
docker compose logs -f server  # 观察启动日志
```

### 步骤 4：启动前端服务

```bash
docker compose up -d web
```

### 步骤 5：验证服务就绪

```bash
# 验证后端
curl -s http://localhost:8080/api/v1/auth/login

# 验证前端
curl -s -o /dev/null -w "%{http_code}" http://localhost:80
```

## 4. 环境变量配置

| 变量 | 默认值 | 生产环境要求 | 说明 |
|------|--------|-------------|------|
| POSTGRES_PASSWORD | docchat123 | **必须修改** | PostgreSQL 密码 |
| JWT_SECRET | changeme-in-production | **必须修改** | JWT 签名密钥（>=32字符） |
| XUNFEI_API_KEY | dev-placeholder | **必须配置** | 讯飞 LLM API Key |

## 5. 回滚方案

### 场景 1：服务启动失败

```bash
# 停止失败的服务
docker compose down server web

# 查看日志定位问题
docker compose logs server

# 修复后重新启动
docker compose up -d server web
```

### 场景 2：数据库迁移失败

```bash
# 停止所有服务
docker compose down

# 检查 Flyway 迁移状态
docker compose up postgres -d
docker compose exec postgres psql -U docchat -c "SELECT * FROM flyway_schema_history"

# 如需回退迁移，手动修复后重启
```

### 场景 3：完全回退

```bash
# 停止所有服务并删除容器（保留数据卷）
docker compose down

# 回退到上一个版本
git checkout <previous-stable-tag>

# 重新构建和部署
mvn clean package -DskipTests
docker compose up -d
```

### 场景 4：数据卷清理（谨慎！）

```bash
# ⚠️ 此操作不可逆，会删除所有数据
docker compose down -v
```

## 6. 监控方案

### 健康检查

| 服务 | 检查方式 | 预期结果 |
|------|---------|---------|
| PostgreSQL | `pg_isready -U docchat` | accepting connections |
| Redis | `redis-cli ping` | PONG |
| Milvus | `curl http://localhost:9091/healthz` | 200 OK |
| Server | `curl http://localhost:8080/actuator/health` | UP（如已启用） |
| Web | `curl http://localhost:80` | 200 OK |

### 日志查看

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f server

# 查看最近 100 行
docker compose logs --tail 100 server
```

## 7. 数据备份方案

```bash
# PostgreSQL 备份
docker compose exec postgres pg_dump -U docchat docchat > backup_$(date +%Y%m%d).sql

# PostgreSQL 恢复
cat backup_20260624.sql | docker compose exec -T postgres psql -U docchat docchat
```

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-24 | 初始版本，MVP 部署方案 |
