# 项目目录结构和分层架构规范

> 本文件定义项目的目录布局、分层架构和模块划分规则。所有新增文件必须遵循此规范。

## 设计原则

本架构特别考虑 **AI Agent 阅读效率**，遵循以下原则：

1. **模块自包含**：每个模块目录是一个完整业务域，Agent 读取一个目录即可理解该域全貌
2. **入口文件导航**：每个关键目录有导航文件（README / package-info），Agent 快速定位
3. **依赖显式化**：模块间依赖在结构中可见，无需追踪 import 才能发现
4. **扁平优于嵌套**：避免过深目录层级，减少 Agent 路径追踪成本
5. **命名即文档**：目录/文件命名直接传达用途，减少推断成本

## 目录结构

```
docchat/                           # 项目根目录
├── .ai/                           # 项目 steering 约束文件
│   ├── product.md                 # 产品定位和功能边界
│   ├── tech.md                    # 技术栈约定
│   ├── structure.md               # 本文件 — 架构规范
│   └── codeRule.md                # 代码规范
├── .claude/                       # Claude Code 配置
├── server/                        # ========== 后端 (Spring Boot) ==========
│   ├── pom.xml                    # Maven 项目配置
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/docchat/
│   │   │   │   ├── DocChatApplication.java    # 应用入口
│   │   │   │   ├── common/                    # ===== 公共层（跨模块共享）=====
│   │   │   │   │   ├── config/                #   全局配置类（Redis、Milvus、Security 等）
│   │   │   │   │   ├── exception/             #   全局异常定义与处理器
│   │   │   │   │   ├── response/              #   统一响应封装（R<T>、PageResult）
│   │   │   │   │   ├── util/                  #   工具类（仅放真正通用的）
│   │   │   │   │   └── constant/              #   全局常量
│   │   │   │   ├── module-tenant/             # ===== 租户与用户管理 =====
│   │   │   │   │   ├── package-info.java      #   模块导航：职责、内部结构、依赖关系
│   │   │   │   │   ├── controller/            #   请求处理 / 路由
│   │   │   │   │   ├── service/               #   业务逻辑
│   │   │   │   │   ├── repository/            #   数据访问 / JPA Repository
│   │   │   │   │   ├── entity/                #   JPA 实体定义
│   │   │   │   │   └── dto/                   #   请求/响应 DTO
│   │   │   │   ├── module-knowledge/          # ===== 知识库管理 =====
│   │   │   │   │   ├── package-info.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── module-task/               # ===== 异步任务处理 =====
│   │   │   │   │   ├── package-info.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── module-chat/               # ===== RAG 对话服务 =====
│   │   │   │   │   ├── package-info.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   └── ChatController.java       # 对话端点（预览+正式共用，鉴权方式区分）
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── ChatService.java           # 对话编排
│   │   │   │   │   │   ├── ChatServiceImpl.java       # 对话编排实现
│   │   │   │   │   │   ├── RetrievalService.java      # 向量检索
│   │   │   │   │   │   └── LlmService.java            # LLM 调用抽象
│   │   │   │   │   ├── aop/
│   │   │   │   │   │   └── ChatStatAspect.java        # 对话统计切面（V1：按鉴权类型决定是否记录）
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── module-widget/             # ===== 聊天组件管理 =====
│   │   │   │   │   ├── package-info.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── module-stat/              # ===== 用量统计 (V1) =====
│   │   │   │       ├── package-info.java
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── entity/
│   │   │   │       └── dto/
│   │   │   │   ├── module-apikey/            # ===== API Key 与访问控制 (V1) =====
│   │   │   │   │   ├── package-info.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   └── module-eval/              # ===== 评测集 (V1) =====
│   │   │   │       ├── package-info.java
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── entity/
│   │   │   │       └── dto/
│   │   │   └── resources/
│   │   │       ├── application.yml            # 主配置
│   │   │       ├── application-dev.yml        # 开发环境
│   │   │       ├── application-prod.yml       # 生产环境
│   │   │       └── db/migration/              # Flyway 迁移脚本
│   │   │           ├── V1__create_tenant_tables.sql
│   │   │           └── V2__create_knowledge_tables.sql
│   │   └── test/java/com/docchat/
│   │       ├── module-tenant/                 # 租户模块测试
│   │       ├── module-knowledge/              # 知识库模块测试
│   │       ├── module-task/                   # 任务模块测试
│   │       ├── module-chat/                   # 对话模块测试
│   │       └── common/                        # 公共层测试
│   └── logs/                                  # 日志目录（gitignore）
│
├── web/                            # ========== 管理后台 (Vue 3) ==========
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── src/
│   │   ├── main.ts                 # 应用入口
│   │   ├── App.vue                 # 根组件
│   │   ├── api/                    # 后端 API 调用封装（按模块分文件）
│   │   │   ├── tenant.ts           #   租户相关 API
│   │   │   ├── knowledge.ts        #   知识库相关 API
│   │   │   ├── task.ts             #   任务相关 API
│   │   │   └── chat.ts             #   对话相关 API
│   │   ├── views/                  # 页面级组件（按模块分目录）
│   │   │   ├── tenant/             #   租户管理页面
│   │   │   ├── knowledge/          #   知识库管理页面
│   │   │   ├── task/               #   任务状态页面
│   │   │   └── widget/             # 聊天组件配置页面（含 iframe 预览窗口，V1 增加模拟对话+刷新重置）
│   │   ├── components/             # 通用可复用组件
│   │   │   ├── layout/             #   布局组件（Header、Sidebar 等）
│   │   │   └── common/             #   通用业务组件
│   │   ├── stores/                 # Pinia 状态管理（按模块分文件）
│   │   │   ├── tenant.ts
│   │   │   ├── knowledge.ts
│   │   │   └── user.ts
│   │   ├── router/                 # 路由配置
│   │   │   └── index.ts
│   │   ├── utils/                  # 工具函数
│   │   ├── hooks/                  # 组合式函数 (useXxx)
│   │   ├── types/                  # TypeScript 类型定义（按模块分文件）
│   │   │   ├── tenant.d.ts
│   │   │   ├── knowledge.d.ts
│   │   │   └── api.d.ts            #   通用 API 响应类型
│   │   └── assets/                 # 静态资源
│   └── public/                     # 公共静态文件
│
├── packages/                       # ========== 独立子包 ==========
│   └── chat-widget/                # 嵌入式聊天组件（输出 JS 脚本）
│       ├── package.json
│       ├── vite.config.ts          # Vite lib mode，输出 IIFE
│       ├── src/
│       │   ├── main.ts             # 组件入口
│       │   ├── ChatWidget.ts       # 主组件类（V1：新增 postMessage 监听 + reset() 方法）
│       │   ├── api.ts              # 后端 API 调用
│       │   ├── types.ts            # 类型定义
│       │   └── styles/             # 隔离样式
│       │       └── widget.css
│       └── dist/                   # 构建产物（gitignore）
│
├── docker/                         # ========== Docker 配置 ==========
│   ├── Dockerfile.server           # 后端镜像
│   ├── Dockerfile.web              # 前端镜像
│   ├── Dockerfile.milvus           # Milvus 配置（如需自定义）
│   └── docker-compose.yml          # 本地开发/部署编排
│
├── docs/                           # ========== 项目文档 ==========
│   ├── architecture.md             # 架构设计文档
│   ├── api.md                      # API 接口文档
│   └── deployment.md               # 部署指南
│
├── scripts/                        # ========== 工具脚本 ==========
│   ├── setup-dev.sh                # 开发环境初始化
│   └── seed-data.sh                # 测试数据填充
│
├── CLAUDE.md                       # Claude Code 项目说明
├── .gitignore
└── README.md                       # 项目说明（含模块导航索引）
```

## 分层架构

```
┌─────────────────────────────────────────────────────┐
│  Controller / Router                                │
│  ← 请求接收、参数校验（@Valid）、响应格式化（R<T>）   │
│  ← DTO 转换边界：只接收/返回 DTO，不暴露 Entity      │
├─────────────────────────────────────────────────────┤
│  Service                                            │
│  ← 业务逻辑、事务编排、跨模块调用                     │
│  ← LLM 调用、向量检索编排、任务调度                   │
│  ← 内部使用 Entity，对外返回 DTO                     │
├─────────────────────────────────────────────────────┤
│  Repository / Data Access                           │
│  ← 数据库操作（Spring Data JPA Repository）          │
│  ← 向量库操作（Milvus Client 封装）                   │
│  ← 缓存操作（Redis Template 封装）                    │
├─────────────────────────────────────────────────────┤
│  Entity / Model                                     │
│  ← JPA 实体定义、数据库表映射                         │
│  ← Milvus Collection Schema 定义                     │
│  ← 纯数据结构，不含业务逻辑                           │
└─────────────────────────────────────────────────────┘

依赖方向：上层 → 下层（严格单向）
         Controller → Service → Repository → Entity
         禁止反向依赖：Entity 不引用 Service，Service 不引用 Controller
```

## 模块划分

### 模块清单

| 模块 | 目录 | 职责 | 阶段 |
|------|------|------|------|
| module-tenant | `server/.../module-tenant/` | 注册登录、租户工作空间、团队成员、角色权限 | MVP |
| module-knowledge | `server/.../module-knowledge/` | 文档上传/删除、切分策略、版本管理 | MVP |
| module-task | `server/.../module-task/` | 异步任务提交、状态查询、失败重试 | MVP |
| module-chat | `server/.../module-chat/` | RAG 问答、向量检索、LLM 调用、预览/正式对话共用端点 | MVP |
| module-widget | `server/.../module-widget/` | 聊天组件配置、JS 脚本生成、外观设置、预览窗口支持 | MVP |
| module-stat | `server/.../module-stat/` | API 用量统计、Token 消耗统计 | V1 |
| module-apikey | `server/.../module-apikey/` | API Key 生成/吊销、每日调用次数硬限制 | V1 |
| module-eval | `server/.../module-eval/` | 评测集管理、自动评测检索 Hit Rate、评测结果历史对比 | V1 |

### 模块间依赖关系

```
module-tenant ← (被所有模块依赖，提供租户上下文)
     ↑
module-knowledge → module-task (文档上传后触发切分任务)
     ↑                    ↑
module-chat ← module-knowledge (对话时检索知识库)
     ↑
module-widget ← module-chat (组件嵌入对话能力)
     ↑
module-apikey ← module-tenant (API Key 绑定租户)
     ↑
module-stat ← module-chat (AOP 切面统计正式对话用量，预览对话不计入)
     ↑
module-eval ← module-knowledge (评测知识库检索质量)
```

**规则**：
- 模块间调用必须通过 **Service 层**，禁止直接访问其他模块的 Repository
- module-tenant 是基础模块，其他模块可依赖它，但它不依赖任何业务模块
- module-chat 依赖 module-knowledge 的 Service 进行检索，但不直接操作知识库 Repository
- 跨模块调用方向遵循依赖图，禁止循环依赖

### V1 预览对话架构规则

**同路径原则**：预览对话与正式访客对话走**完全相同的后端代码路径**，确保"预览 OK 则终端一定 OK"。

1. **单一端点**：`POST /api/v1/chat/conversations` 是唯一的对话端点，预览和正式共用
2. **鉴权方式区分**：
   - 预览调用：JWT Token（管理员身份），通过 `JwtAuthenticationFilter` 解析
   - 正式调用：widget_token（访客身份），通过 ChatController 自行解析
3. **统计隔离**：`ChatStatAspect`（AOP 切面）拦截对话方法，根据当前鉴权类型决定是否记录用量——JWT 鉴权跳过，widget_token 鉴权记录
4. **ChatService 零感知**：`ChatService.converse()` 内部不区分预览/正式，保证代码路径完全一致
5. **预览窗口**：管理后台 WidgetView.vue 用 **iframe 嵌入真实 ChatWidget**，外观配置变更通过 `postMessage` 同步，刷新重置通过 `postMessage('reset')` 实现
6. **ChatWidget 新增能力**：
   - `postMessage` 监听器：接收 `config-update`（热更新外观）和 `reset`（清空对话恢复初始状态）
   - `reset()` 方法：清空消息列表，恢复欢迎语

### 模块导航文件规范

每个模块目录必须有 `package-info.java`，内容包含：

```java
/**
 * 知识库管理模块 (module-knowledge)
 *
 * 职责：文档上传/删除、自动切分向量化、版本记录与回滚
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理文档上传/删除/查询请求
 * - service/    : 核心业务逻辑，切分策略、版本管理、触发任务
 * - repository/ : JPA 数据访问 + Milvus 向量操作封装
 * - entity/     : Knowledge、KnowledgeVersion 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant (获取租户上下文)
 * - 依赖 module-task (文档上传后触发切分向量化任务)
 * - 被 module-chat 依赖 (对话时检索知识库)
 */
package com.docchat.module-knowledge;
```

## 前端目录规则

- **views/** 按后端模块名分目录，与后端一一对应
- **api/** 按后端模块名分文件，每个文件封装对应模块的所有 API 调用
- **stores/** 按模块分文件，与 api/ 对应
- **types/** 按模块分文件，DTO 类型与后端保持命名一致
- **components/** 只放真正跨页面复用的组件，页面专属组件放 views/ 对应目录下

## 共享代码规则

### 后端 common/ 规则

- **只放真正跨 3+ 模块使用的代码**，2 个模块使用的放调用方模块
- common/ 下按职责分子目录：config / exception / response / util / constant
- 禁止在 common/ 中放置业务逻辑，只放技术性公共代码

### 前端公共规则

- **components/layout/** ：布局组件（Header、Sidebar、Footer）
- **components/common/** ：通用业务组件（需跨 3+ 页面使用）
- **hooks/** ：组合式函数，按功能命名 `useXxx.ts`
- **utils/** ：纯工具函数，无业务逻辑依赖

## 测试目录规则

- 测试目录结构与源码目录结构 **一一对应**
- 测试类命名：`{被测类}Test.java`，如 `KnowledgeServiceTest.java`
- 集成测试使用 `@SpringBootTest` + TestContainers
- 单元测试使用 `@ExtendWith(MockitoExtension.class)`

## 配置文件规则

- `application.yml` — 公共配置，不含敏感信息
- `application-dev.yml` — 开发环境配置（可含本地连接串）
- `application-prod.yml` — 生产环境配置（敏感值通过环境变量注入）
- `db/migration/` — Flyway 迁移脚本，按版本号递增，禁止修改已执行脚本

## Agent 导航约定

为提升 AI Agent 的项目阅读效率，除 `package-info.java` 外，还遵循：

1. **README.md 导航**：项目根目录 README.md 包含模块索引，列出每个模块的职责和目录路径
2. **API 路径前缀**：每个模块的 API 路径以模块名开头，如 `/api/knowledge/...`、`/api/task/...`
3. **DTO 命名约定**：`{Entity}{Action}Request/Response`，如 `KnowledgeUploadRequest`、`TaskStatusResponse`
4. **Service 接口优先**：核心 Service 定义接口 + 实现，方便 Agent 通过接口快速理解能力边界

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-23 | 初始版本：完成目录结构、分层架构、模块划分、Agent 导航约定 |
| 2026-06-26 | V1 架构更新：① module-chat 新增 aop/ 子目录（ChatStatAspect）；② 新增"V1 预览对话架构规则"（同路径原则、鉴权区分、统计隔离、iframe 预览、ChatWidget postMessage+reset）；③ 前端 widget 视图描述更新 |
