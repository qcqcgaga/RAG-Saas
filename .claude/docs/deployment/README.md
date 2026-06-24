# 部署与配置

> Docker Compose 部署，Nginx 反向代理，环境变量注入敏感配置

## 配置文件层级

```
application.yml          → 公共配置（不含敏感信息）
application-dev.yml      → 开发环境（本地连接串，可硬编码）
application-prod.yml     → 生产环境（全部用 ${ENV_VAR} 注入）
```

## 环境变量清单

| 变量 | 说明 | 开发默认值 | 生产必填 |
|------|------|-----------|----------|
| `JWT_SECRET` | JWT 签名密钥 | dev-only-secret-key... | ✅ |
| `JWT_EXPIRATION` | JWT 过期时间(秒) | 86400 | |
| `DATABASE_URL` | PostgreSQL 连接串 | localhost:5432/docchat | ✅ |
| `DATABASE_USERNAME` | 数据库用户名 | docchat | ✅ |
| `DATABASE_PASSWORD` | 数据库密码 | docchat_dev | ✅ |
| `REDIS_HOST` | Redis 地址 | localhost | ✅ |
| `REDIS_PORT` | Redis 端口 | 6379 | |
| `REDIS_PASSWORD` | Redis 密码 | - | ✅ |
| `MILVUS_URI` | Milvus URI | http://localhost:19530 | ✅ |
| `STORAGE_PATH` | 文件存储路径 | ./uploads | |
| `XUNFEI_API_KEY` | 讯飞 API Key | dev-placeholder | ✅ |
| `XUNFEI_API_URL` | 讯飞 API 地址 | https://maas-coding-api... | ✅ |

## Docker Compose 架构

```
Nginx(:80) → Server(:8080) / 静态前端
                ↓
        PostgreSQL(:5432) + Redis(:6379) + Milvus(:19530)
```

## Nginx 配置要点

- `/api/` → 反向代理到 server:8080
- SSE 支持: `proxy_buffering off` + `proxy_read_timeout 120s`
- SPA 路由: `try_files $uri /index.html`
- 静态资源缓存: `/assets/` 30天

## 开发环境启动

```bash
./scripts/setup-dev.sh
# 构建: 后端(mvn) + 前端(pnpm) + chat-widget(pnpm build)
# 启动: docker compose up -d
```

## Flyway 数据库迁移

```
server/src/main/resources/db/migration/
├── V1__create_tenant_tables.sql
├── V2__create_knowledge_tables.sql
├── V3__refine_data_model.sql
└── V4__fix_and_supplement.sql

server/src/main/resources/db/seed/
└── R__seed_dev_data.sql  # 开发环境种子数据
```

- 禁止修改已执行的迁移脚本
- 新增迁移按版本号递增
- 开发环境自动加载 seed 数据

## 详细文档

- [pitfalls.md](pitfalls.md)
