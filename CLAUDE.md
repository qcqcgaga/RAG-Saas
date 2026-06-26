# DocChat — 文档智能客服 SaaS

> 面向独立开发者/小团队，上传产品文档/FAQ，5 分钟生成可嵌入网站的聊天组件。

## 快速导航

| 模块 | 目录 | 文档 |
|------|------|------|
| 后端-公共层 | `server/.../common/` | [docs/common/README.md](.claude/docs/common/README.md) |
| 后端-租户管理 | `server/.../module_tenant/` | [docs/module-tenant/README.md](.claude/docs/module-tenant/README.md) |
| 后端-知识库管理 | `server/.../module_knowledge/` | [docs/module-knowledge/README.md](.claude/docs/module-knowledge/README.md) |
| 后端-异步任务 | `server/.../module_task/` | [docs/module-task/README.md](.claude/docs/module-task/README.md) |
| 后端-RAG对话 | `server/.../module_chat/` | [docs/module-chat/README.md](.claude/docs/module-chat/README.md) |
| 后端-聊天组件 | `server/.../module_widget/` | [docs/module-widget/README.md](.claude/docs/module-widget/README.md) |
| 管理后台(Vue3) | `web/` | [docs/web-admin/README.md](.claude/docs/web-admin/README.md) |
| 嵌入式聊天组件 | `packages/chat-widget/` | [docs/chat-widget/README.md](.claude/docs/chat-widget/README.md) |
| 部署配置 | `docker/` + `scripts/` | [docs/deployment/README.md](.claude/docs/deployment/README.md) |

## AI Steering 约束

所有代码生成和修改**必须遵守** `.ai/` 目录下的约束文件：

| 优先级 | 文件 | 用途 |
|--------|------|------|
| 1 | [.ai/codeRule.md](.ai/codeRule.md) | 代码硬性规则（命名、复杂度、安全） |
| 2 | [.ai/structure.md](.ai/structure.md) | 目录结构和分层架构规范 |
| 3 | [.ai/tech.md](.ai/tech.md) | 技术栈约束（Java 21 / Spring Boot 3.3 / Vue 3 / PostgreSQL / Milvus） |
| 4 | [.ai/product.md](.ai/product.md) | 功能边界（"不做"列表不可违反） |

## 技术栈速查

```
后端: Java 21 + Spring Boot 3.3 + JPA + PostgreSQL 16 + Milvus 2.4 + Redis 7
前端: TypeScript + Vue 3 + Ant Design Vue 4 + Pinia + Vite 6
组件: TypeScript + Vite (lib mode, IIFE 输出)
工具: Maven 3.9 + pnpm 9 + Flyway + Docker Compose
```

## 分层架构

```
Controller → Service → Repository → Entity
  ↓ DTO入     ↓ 业务逻辑  ↓ 数据访问   ↓ 纯数据
  ↓ @Valid     ↓ 跨模块调用 ↓ JPA+Milvus ↓ 无逻辑
```

**依赖方向严格单向**，禁止反向依赖。模块间调用通过 Service 层，禁止直接访问其他模块 Repository。

## 模块依赖

```
module-tenant ← (被所有模块依赖)
module-knowledge → module-task (文档上传触发切分任务)
module-chat ← module-knowledge (对话时检索知识库)
module-widget ← module-chat (组件嵌入对话能力)
```

## 关键规则

- **多租户隔离**：TenantContext + AOP，禁止手动传 tenant_id
- **统一响应**：`R<T>` 封装，错误码 `{MODULE}_{ERROR_TYPE}`
- **文件上传**：白名单(PDF/MD/TXT) + 50MB上限 + UUID重命名
- **JWT无状态鉴权**：管理后台用JWT，聊天组件用 widget_token
- **对话即焚**：不持久化终端访客对话

## 命名差异说明

`.ai/structure.md` 定义模块名用连字符（`module-tenant`），但 Java 包名不允许连字符。
实际代码中有代码的模块使用**下划线**：`module_tenant`、`module_knowledge`、`module_chat`、`module_widget`。
文档和配置统一用连字符，与 `.ai/` 约束保持一致。

## 强制约束

- **🚫 禁止向 LLM 提交图片**：任何时候、任何场景下，严禁将图片（截图、照片、扫描件等）作为输入提交给 LLM。仅允许使用纯文本输入。违反此规则视为严重违规。

## Agent 行为规范

系统蜂鸣提示（强制）：人工环节和任务完成前必须播放：
```bash
powershell -Command "[System.Media.SystemSounds]::Asterisk.Play()"
```

## 当前状态

- [x] 产品功能 / 技术栈 / 架构设计 / 项目骨架
- [x] MVP：租户管理 + 知识库管理 + 异步任务 + RAG对话 + 聊天组件
- [ ] V1：API Key + 用量统计 + 评测集 + 聊天组件预览与模拟测试 + LLM API配置
