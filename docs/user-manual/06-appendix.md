# 附录

## 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| 租户 | Tenant | DocChat 的工作空间单元，每个注册用户自动创建一个租户，数据严格隔离 |
| 知识库 | Knowledge Base | 存储租户上传文档和向量化数据的知识仓库 |
| RAG | Retrieval-Augmented Generation | 检索增强生成，先检索相关文档再生成回答的 AI 问答模式 |
| 向量化 | Embedding / Vectorization | 将文本转换为数值向量的过程，用于语义相似度计算 |
| 切分 | Chunking / Splitting | 将长文档按语义段落分割为小片段的过程 |
| Milvus | Milvus | 开源向量数据库，用于存储和检索文档向量 |
| SSE | Server-Sent Events | 服务器推送事件，用于实现对话流式输出 |
| JWT | JSON Web Token | 无状态鉴权令牌，用于管理后台身份验证 |
| Widget Token | Widget Token | 聊天组件专用鉴权令牌，与 JWT 隔离 |
| Flyway | Flyway | 数据库版本迁移工具，管理 DDL 变更 |
| slug | Slug | 租户的唯一 URL 友好标识，仅包含字母、数字和连字符 |
| 对话即焚 | Ephemeral Chat | 终端访客对话不持久化存储的策略 |
| 来源引用 | Source Citation | AI 回答附带引用的文档名称和段落位置 |

## 更新日志

### MVP (2026-06)

**新增功能：**

- 租户管理：注册登录、租户设置、团队成员邀请、角色权限管理
- 知识库管理：文档上传（PDF/MD/TXT）、自动切分向量化、版本管理与回滚
- 异步任务：文档处理状态跟踪、进度查看、失败重试
- RAG 对话：基于知识库的智能问答、SSE 流式输出、来源引用
- 聊天组件：嵌入脚本生成、外观配置（品牌色/欢迎语/图标）、令牌管理
- 基础设施：Docker Compose 全栈部署、PostgreSQL + Redis + Milvus 编排

## API 端点速查

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
| GET | `/api/v1/knowledge/documents` | 文档列表（支持分页、搜索、状态筛选） |
| POST | `/api/v1/knowledge/documents` | 上传文档 |
| GET | `/api/v1/knowledge/documents/{id}` | 获取文档详情 |
| DELETE | `/api/v1/knowledge/documents/{id}` | 删除文档 |
| GET | `/api/v1/knowledge/documents/{id}/versions` | 获取版本历史 |
| POST | `/api/v1/knowledge/documents/{docId}/versions/{verId}/rollback` | 版本回滚 |

### 异步任务（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/tasks` | 任务列表（分页） |
| GET | `/api/v1/tasks/{id}` | 任务详情 |
| POST | `/api/v1/tasks/{id}/retry` | 重试失败任务 |

### 聊天组件（需要 JWT）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/widget/config` | 获取组件配置（通过 token 参数） |
| PUT | `/api/v1/widget/config` | 更新组件配置 |
| GET | `/api/v1/widget/embed-script` | 获取嵌入脚本 |
| POST | `/api/v1/widget/regenerate-token` | 重新生成令牌 |

### 对话（需要 widget_token）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/conversations` | 发起对话（SSE 流式） |

## 参考链接

| 资源 | 链接 |
|------|------|
| Spring Boot 文档 | https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/ |
| Vue 3 文档 | https://vuejs.org/ |
| Ant Design Vue | https://antdv.com/ |
| Milvus 文档 | https://milvus.io/docs |
| PostgreSQL 文档 | https://www.postgresql.org/docs/16/ |
| Redis 文档 | https://redis.io/docs/ |
| Flyway 文档 | https://flywaydb.org/documentation/ |
