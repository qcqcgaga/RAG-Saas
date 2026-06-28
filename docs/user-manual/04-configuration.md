# 配置说明

## 环境变量

DocChat 通过环境变量管理配置，支持 `.env` 文件或 Docker Compose 环境变量。

### 必需配置

| 变量名 | 默认值 | 说明 | 修改方式 |
|--------|--------|------|----------|
| `POSTGRES_PASSWORD` | `docchat123` | PostgreSQL 数据库密码 | `.env` 文件或 Docker Compose environment |
| `JWT_SECRET` | `docchat-prod-jwt-secret-must-be-at-least-32-chars!!` | JWT Token 签名密钥，至少 32 字符 | `.env` 文件 |

> **安全提示**：生产环境务必修改以上默认值，特别是 `JWT_SECRET` 和 `POSTGRES_PASSWORD`。

### 可选配置

| 变量名 | 默认值 | 说明 | 修改方式 |
|--------|--------|------|----------|
| `JWT_EXPIRATION` | `86400` | JWT Token 有效期（秒），默认 24 小时 | `.env` 文件 |
| `STORAGE_PATH` | `./uploads` | 文件上传存储路径 | `.env` 文件 |
| `MILVUS_URI` | `http://localhost:19530` | Milvus 向量数据库连接地址 | `.env` 文件 |

### V1 新增：租户级配置

V1 版本新增了租户级的 LLM 配置，通过管理后台界面设置，无需环境变量：

| 配置项 | 说明 | 设置方式 |
|--------|------|----------|
| LLM API 地址 | 租户自定义 LLM 服务的端点 URL | 管理后台 → LLM 配置 |
| LLM API 密钥 | 租户自定义 LLM 服务的访问密钥 | 管理后台 → LLM 配置 |
| LLM 模型名称 | 使用的模型标识 | 管理后台 → LLM 配置 |

详见 [LLM 配置](03-features/llm-config.md)。

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

#### 服务器端口

```yaml
server:
  port: 8080
```

#### JPA

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # 使用 Flyway 管理 DDL，禁止自动建表
    open-in-view: false      # 禁用 OSIV，避免懒加载问题
    properties:
      hibernate:
        format_sql: true
        default_schema: public
```

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

#### Flyway 数据库迁移

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## 数据库

### PostgreSQL

- **默认数据库名**：`docchat`
- **默认用户名**：`docchat`
- **字符集**：UTF-8
- **数据库迁移**：使用 Flyway 管理，迁移脚本位于 `server/src/main/resources/db/migration/`

V1 新增迁移脚本：

| 脚本 | 说明 |
|------|------|
| `V5__create_v1_tables.sql` | 创建 V1 新表（API Key、统计、评测、LLM 配置等） |
| `V6__alter_tenant_add_daily_limit.sql` | 租户表新增每日限额字段 |
| `V7__add_v1_indexes.sql` | V1 新增索引 |

### Milvus

- **连接方式**：Standalone 模式
- **依赖**：etcd（元数据存储）+ MinIO（对象存储）
- **租户隔离**：每个租户使用独立的 Milvus Collection

### Redis

- **用途**：缓存加速 + 异步任务队列 + 每日限额计数器
- **持久化**：启用 RDB 快照

## 安全配置建议

| 项目 | 建议 |
|------|------|
| JWT Secret | 使用至少 32 字符的随机字符串 |
| 数据库密码 | 使用强密码，避免包含产品名称 |
| API Key | 不要将 API Key 硬编码在代码中，使用环境变量或配置中心 |
| LLM API 密钥 | 通过管理后台配置，不要写入环境变量或代码 |
| HTTPS | 生产环境务必启用 HTTPS |
| 网络隔离 | 后端 API 和数据库不应直接暴露到公网 |
| CORS | 配置允许的来源域名，避免使用 `*` |

## V1 API 端点速查

### 认证（无需 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/register` | 注册新账号 |
| POST | `/api/v1/auth/login` | 登录获取 Token |

### 租户管理（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/tenants/current` | 获取当前租户信息 |
| PUT | `/api/v1/tenants/current` | 修改当前租户信息 |
| GET | `/api/v1/tenants/members` | 获取成员列表 |
| POST | `/api/v1/tenants/members` | 邀请成员 |
| PUT | `/api/v1/tenants/members/{userId}/role` | 修改成员角色 |
| DELETE | `/api/v1/tenants/members/{userId}` | 移除成员 |

### 知识库管理（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/knowledge` | 获取知识库信息 |
| PUT | `/api/v1/knowledge` | 更新知识库信息 |
| GET | `/api/v1/knowledge/documents` | 文档列表 |
| POST | `/api/v1/knowledge/documents` | 上传文档 |
| GET | `/api/v1/knowledge/documents/{id}` | 获取文档详情 |
| DELETE | `/api/v1/knowledge/documents/{id}` | 删除文档 |
| GET | `/api/v1/knowledge/documents/{id}/versions` | 获取版本历史 |
| POST | `/api/v1/knowledge/documents/{docId}/versions/{verId}/rollback` | 版本回滚 |

### 异步任务（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/tasks` | 任务列表 |
| GET | `/api/v1/tasks/{id}` | 任务详情 |
| POST | `/api/v1/tasks/{id}/retry` | 重试失败任务 |

### 聊天组件（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/widget/config` | 获取组件配置 |
| PUT | `/api/v1/widget/config` | 更新组件配置 |
| GET | `/api/v1/widget/embed-script` | 获取嵌入脚本 |
| POST | `/api/v1/widget/regenerate-token` | 重新生成令牌 |

### 对话（需要 API Key 或 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/conversations` | 发起对话（SSE 流式） |

### API Key 管理（需要 JWT，V1 新增）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/api-keys` | 创建 API Key |
| GET | `/api/v1/api-keys` | 列出所有 API Key |
| DELETE | `/api/v1/api-keys/{keyId}` | 吊销 API Key |
| PUT | `/api/v1/api-keys/{keyId}/name` | 重命名 API Key |

### 用量统计（需要 JWT，V1 新增）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/stats/overview` | 用量概览（period: 7d/30d） |
| GET | `/api/v1/stats/daily` | 每日统计（startDate, endDate） |
| GET | `/api/v1/stats/trend` | 趋势统计（period, metric: calls/tokens/conversations） |

### 评测集管理（需要 JWT，V1 新增）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/eval/sets` | 创建评测集 |
| GET | `/api/v1/eval/sets` | 列出评测集 |
| GET | `/api/v1/eval/sets/{setId}` | 获取评测集详情 |
| PUT | `/api/v1/eval/sets/{setId}` | 更新评测集 |
| DELETE | `/api/v1/eval/sets/{setId}` | 删除评测集 |
| POST | `/api/v1/eval/sets/{setId}/pairs` | 添加问答对 |
| GET | `/api/v1/eval/sets/{setId}/pairs` | 列出问答对 |
| DELETE | `/api/v1/eval/sets/{setId}/pairs/{pairId}` | 删除问答对 |
| POST | `/api/v1/eval/sets/{setId}/pairs/import` | 批量导入问答对 |
| POST | `/api/v1/eval/sets/{setId}/run` | 执行评测 |
| GET | `/api/v1/eval/sets/{setId}/results` | 列出评测结果 |
| GET | `/api/v1/eval/sets/{setId}/results/{resultId}` | 获取评测结果详情 |

### LLM 配置（需要 JWT，V1 新增）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/llm-config` | 获取当前 LLM 配置 |
| PUT | `/api/v1/llm-config` | 更新 LLM 配置 |
| DELETE | `/api/v1/llm-config` | 删除配置（恢复系统默认） |
| POST | `/api/v1/llm-config/test` | 测试 LLM 连通性 |
