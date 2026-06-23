# DocChat — 文档智能客服 SaaS

面向独立开发者/小团队，上传文档即可生成可嵌入的聊天组件，5 分钟上线。

## 快速开始

```bash
# 一键初始化开发环境
./scripts/setup-dev.sh

# 或手动启动
cd docker && docker compose up -d   # 启动基础设施
cd server && mvn spring-boot:run    # 启动后端
cd web && pnpm dev                  # 启动前端
```

## 项目结构

```
docchat/
├── server/                 # 后端 — Spring Boot 3 + Java 21
├── web/                    # 管理后台 — Vue 3 + Ant Design Vue
├── packages/chat-widget/   # 嵌入式聊天组件 — TypeScript IIFE
├── docker/                 # Docker 配置（Compose 全栈编排）
├── docs/                   # 项目文档
├── scripts/                # 工具脚本
└── .ai/                    # 项目约束文件（技术栈、架构、规范）
```

## 模块导航

| 模块 | 后端路径 | 前端路径 | 职责 | 阶段 |
|------|----------|----------|------|------|
| 租户管理 | `server/.../module-tenant/` | `web/src/views/tenant/` | 注册登录、租户、角色权限 | MVP |
| 知识库 | `server/.../module-knowledge/` | `web/src/views/knowledge/` | 文档上传、切分向量化 | MVP |
| 异步任务 | `server/.../module-task/` | `web/src/views/task/` | 任务状态、进度、重试 | MVP |
| RAG 对话 | `server/.../module-chat/` | — | 问答、向量检索、LLM | MVP |
| 聊天组件 | `server/.../module-widget/` | `web/src/views/widget/` | 配置、脚本生成 | MVP |
| 用量统计 | `server/.../module-stat/` | — | 调用量、Token 统计 | V1 |
| API Key | `server/.../module-apikey/` | — | 生成/吊销、调用限制 | V1 |
| 评测集 | `server/.../module-eval/` | — | 问答对、Hit Rate 评测 | V1 |

## 技术栈

| 层面 | 选型 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.3 + Spring Data JPA + PostgreSQL 16 |
| 向量数据库 | Milvus 2.4 |
| 缓存/队列 | Redis 7 |
| 前端 | Vue 3 + TypeScript + Ant Design Vue 4 + Pinia |
| 聊天组件 | TypeScript + Vite (lib mode, IIFE) |
| LLM | 讯飞 Coding Plan API |
| 部署 | Docker Compose |

## 约束文件

所有开发必须遵守 `.ai/` 目录下的约束文件：

- [`.ai/product.md`](.ai/product.md) — 产品定位和功能边界
- [`.ai/tech.md`](.ai/tech.md) — 技术栈约定
- [`.ai/structure.md`](.ai/structure.md) — 目录结构和架构规范
- [`.ai/codeRule.md`](.ai/codeRule.md) — 代码规范

## 里程碑

- [x] 产品定位与功能边界
- [x] 技术选型与架构设计
- [x] 项目骨架初始化
- [ ] MVP：租户管理 + 知识库管理 + 异步任务处理 + RAG 对话服务 + 聊天组件嵌入
- [ ] V1：API Key 与访问控制 + 用量统计 + 评测集
