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
| JWT | JSON Web Token | 无状态鉴权令牌，用于管理后台身份验证，`eyJ` 前缀 |
| API Key | API Key | V1 新增的聊天组件鉴权令牌，`dc_` 前缀，支持统计和限额 |
| Widget Token | Widget Token | MVP 版本的聊天组件鉴权令牌，V1 过渡期兼容 |
| Flyway | Flyway | 数据库版本迁移工具，管理 DDL 变更 |
| slug | Slug | 租户的唯一 URL 友好标识，仅包含字母、数字和连字符 |
| 对话即焚 | Ephemeral Chat | 终端访客对话不持久化存储的策略 |
| 来源引用 | Source Citation | AI 回答附带引用的文档名称和段落位置 |
| Hit Rate | Hit Rate | 评测命中率，检索结果中包含期望文档的比例 |
| 评测集 | Eval Set | 包含问答对的评测集合，用于量化 RAG 检索质量 |
| LLM | Large Language Model | 大语言模型，用于生成对话回答 |
| 双鉴权 | Dual Auth | V1 新增，对话接口同时支持 API Key 和 JWT 两种鉴权方式 |
| 每日限额 | Daily Quota | 每日对话调用次数硬限制，防止滥用 |

## 更新日志

### V1 (2026-06)

**新增功能：**

- API Key 管理：创建/吊销/重命名 API Key，每租户最多 5 个，每日调用限额
- 用量统计：概览（7d/30d）、每日明细、趋势图（calls/tokens/conversations），仅统计 API Key 鉴权对话
- 评测集管理：创建评测集、添加问答对（最多 50 对）、批量导入、执行评测（Hit Rate）、查看评测结果与详情、历史结果对比
- LLM 配置：租户级 LLM API 配置、连通性测试、删除配置恢复系统默认
- 聊天组件预览：iframe 预览窗口、实时预览外观配置、模拟终端访客问答、刷新重置
- 双鉴权模式：对话接口支持 API Key（`dc_` 前缀）和 JWT（`eyJ` 前缀）两种鉴权
- 嵌入脚本升级：新增 `data-api-key` 参数，旧版 `data-token` 过渡期兼容
- 数据库迁移：V5-V7 Flyway 迁移脚本，新增 V1 相关表和索引

**改进：**

- 对话鉴权：从单一 widget_token 升级为双鉴权模式
- 统计能力：从无统计到完整的用量统计体系
- 质量保障：新增评测集功能，RAG 检索质量可量化可迭代
- 灵活性：支持租户自定义 LLM 供应商和模型

### MVP (2026-06)

**新增功能：**

- 租户管理：注册登录、租户设置、团队成员邀请、角色权限管理
- 知识库管理：文档上传（PDF/MD/TXT）、自动切分向量化、版本管理与回滚
- 异步任务：文档处理状态跟踪、进度查看、失败重试
- RAG 对话：基于知识库的智能问答、SSE 流式输出、来源引用
- 聊天组件：嵌入脚本生成、外观配置（品牌色/欢迎语/图标）、令牌管理
- 基础设施：Docker Compose 全栈部署、PostgreSQL + Redis + Milvus 编排

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
| OpenAI API 文档 | https://platform.openai.com/docs/api-reference |
