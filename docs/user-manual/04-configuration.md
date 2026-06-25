# 配置说明

## 环境变量

DocChat 通过环境变量管理配置，支持 `.env` 文件或 Docker Compose 环境变量。

### 必需配置

| 变量名 | 默认值 | 说明 | 修改方式 |
|--------|--------|------|----------|
| `POSTGRES_PASSWORD` | `docchat123` | PostgreSQL 数据库密码 | `.env` 文件或 Docker Compose environment |
| `JWT_SECRET` | `docchat-prod-jwt-secret-must-be-at-least-32-chars!!` | JWT Token 签名密钥，至少 32 字符 | `.env` 文件 |
| `XUNFEI_API_KEY` | `dev-placeholder` | 讯飞 Coding Plan API 密钥 | `.env` 文件 |

> **⚠️ 安全提示**：生产环境务必修改以上默认值，特别是 `JWT_SECRET` 和 `POSTGRES_PASSWORD`。

### 可选配置

| 变量名 | 默认值 | 说明 | 修改方式 |
|--------|--------|------|----------|
| `JWT_EXPIRATION` | `86400` | JWT Token 有效期（秒），默认 24 小时 | `.env` 文件 |
| `STORAGE_PATH` | `./uploads` | 文件上传存储路径 | `.env` 文件 |
| `MILVUS_URI` | `http://localhost:19530` | Milvus 向量数据库连接地址 | `.env` 文件 |

## Docker Compose 配置

### 服务端口映射

| 服务 | 容器端口 | 主机端口 | 说明 |
|------|----------|----------|------|
| PostgreSQL | 5432 | 5432 | 数据库 |
| Redis | 6379 | 6379 | 缓存/队列 |
| etcd | 2379 | 2379 | Milvus 依赖 |
| MinIO | 9000/9001 | 9000/9001 | Milvus 存储 / 控制台 |
| Milvus | 19530/9091 | 19530/9091 | 向量数据库 / 健康检查 |
| 后端服务 | 8080 | 8080 | Spring Boot API |
| 前端界面 | 80 | 80 | Nginx 静态资源 |

### 数据持久化

Docker Compose 使用命名卷持久化数据：

| 卷名 | 挂载点 | 用途 |
|------|--------|------|
| `postgres_data` | `/var/lib/postgresql/data` | PostgreSQL 数据 |
| `redis_data` | `/data` | Redis 持久化 |
| `etcd_data` | `/etcd` | etcd 数据 |
| `minio_data` | `/minio_data` | MinIO 对象存储 |
| `milvus_data` | `/var/lib/milvus` | Milvus 向量数据 |
| `uploads_data` | `/app/uploads` | 用户上传文件 |

## 应用配置

### 后端配置文件

后端使用 Spring Boot 配置体系，配置文件位于 `server/src/main/resources/`：

| 文件 | 用途 |
|------|------|
| `application.yml` | 通用配置（端口、JPA、文件上传等） |
| `application-dev.yml` | 开发环境配置（数据库连接、Redis、Milvus 等） |
| `application-prod.yml` | 生产环境配置 |

### 关键配置项

#### 文件上传

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB      # 单文件最大 50MB
      max-request-size: 50MB    # 请求最大 50MB
```

#### JWT

```yaml
docchat:
  jwt:
    secret: ${JWT_SECRET}         # 签名密钥
    expiration: ${JWT_EXPIRATION:86400}  # 有效期（秒）
```

#### 文件存储

```yaml
docchat:
  storage:
    path: ${STORAGE_PATH:./uploads}  # 文件存储路径
```

#### Milvus

```yaml
docchat:
  milvus:
    uri: ${MILVUS_URI:http://localhost:19530}  # Milvus 连接地址
```

## 数据库

### PostgreSQL

- **默认数据库名**：`docchat`
- **默认用户名**：`docchat`
- **字符集**：UTF-8
- **数据库迁移**：使用 Flyway 管理，迁移脚本位于 `server/src/main/resources/db/migration/`

### Milvus

- **连接方式**：Standalone 模式
- **依赖**：etcd（元数据存储）+ MinIO（对象存储）
- **租户隔离**：每个租户使用独立的 Milvus Collection

### Redis

- **用途**：缓存加速 + 异步任务队列
- **持久化**：启用 RDB 快照

## 安全配置建议

| 项目 | 建议 |
|------|------|
| JWT Secret | 使用至少 32 字符的随机字符串 |
| 数据库密码 | 使用强密码，避免包含产品名称 |
| API Key | 不要将 API Key 硬编码在代码中，使用环境变量 |
| HTTPS | 生产环境务必启用 HTTPS |
| 网络隔离 | 后端 API 和数据库不应直接暴露到公网 |
