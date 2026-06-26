# DocChat — Agent 驱动的瀑布软件开发实验

> **这不是一个普通的开源项目** — 这是一个用 AI Agent 模拟真实大厂瀑布式软件开发全流程的实验性项目。一个 AI，13 个流程环节，6 个卡点门禁，用户只做评审决策。

---

## 🧪 实验本质

DocChat 项目同时承载两个身份：

| 身份 | 说明 |
|------|------|
| **产品** | 面向独立开发者/小团队的文档智能客服 SaaS，上传产品文档/FAQ，5 分钟生成可嵌入网站的聊天组件 |
| **实验** | 探索 AI Agent 能否独立执行从需求分析到部署上线的完整瀑布软件开发流程，人类仅作为评审者介入 |

**核心假设**：如果把大厂软件开发流程（需求→设计→编码→测试→部署）完整地交给一个 AI Agent 执行，人类只做卡点评审，能否产出可交付的软件产品？

---

## ✨ 实验特色

### 1. 完整的 14 环节瀑布流程

Agent 严格按照企业级瀑布模型推进，不跳步、不省略：

```
需求分析 → 需求评审★ → 技术方案设计 → 技术方案评审★ → 详细设计 → 详细设计评审★
→ 编码实现 → 代码评审★ → 单元测试 → 集成测试 → 人工测试 → 测试评审★ → 部署上线 → 上线验证★
```

★ = 卡点门禁，必须人工评审通过才能推进

### 2. 7 道卡点门禁（Gate）

每个卡点有结构化的检查清单，按严重度分级（blocker / major / minor）：

| 门禁 | 位置 | 核心把关内容 |
|------|------|-------------|
| G1 需求评审 | 需求分析后 | 需求清晰无歧义、验收标准可量化、不做列表已定义 |
| G2 技术方案评审 | 技术设计后 | 架构可行、选型合理、数据模型完整、安全方案覆盖 |
| G3 详细设计评审 | 详细设计后 | 模块职责清晰、SOLID 原则、时序图覆盖核心流程 |
| G4 代码评审 | 编码完成后 | 命名规范、安全校验、编译构建通过、应用可启动 |
| G5.5 人工测试评审 | 人工测试后 | P0/P1 用户故事验证通过、发现的问题已修复 |
| G5 测试评审 | 测试完成后 | P0/P1 缺陷归零、覆盖率达标、性能达标 |
| G6 上线验证 | 部署上线后 | 核心功能可用、性能正常、监控告警就绪 |

**评审通过条件**：所有 blocker 通过 + major 通过率 ≥ 80%。评审不通过则回退到指定环节，真正实现"质量卡点"。

### 3. 人类是监督者，不是执行者

```
传统模式：人类执行 + 人类评审
本实验：  Agent 执行 + 人类评审
```

- Agent 负责所有文档生成、代码编写、测试执行、部署操作
- 人类只负责在每个卡点做 pass/fail 决策
- 评审不通过时，Agent 自动回退并修正

### 4. 约束文件驱动的 AI 行为

项目通过 `.ai/` 目录的 4 个约束文件，为 Agent 划定"能做什么、不能做什么"的边界：

| 优先级 | 约束文件 | 作用 |
|--------|----------|------|
| 1 | `.ai/codeRule.md` | 代码硬性规则（命名、复杂度、安全） |
| 2 | `.ai/structure.md` | 目录结构和分层架构规范 |
| 3 | `.ai/tech.md` | 技术栈约束（Java 21 / Spring Boot 3.3 / Vue 3 / PostgreSQL / Milvus） |
| 4 | `.ai/product.md` | 产品功能边界（"不做"列表不可违反） |

### 5. 经验沉淀机制

流程执行中发现的最佳实践和教训，自动记录到 `.dev-pipeline/tools-and-lessons.yaml`。例如：

> **L-DEPLOY-004**: 编译通过 ≠ 能运行。javac 宽松但 Spring 扫描会 Bean 冲突，必须实际启动应用验证。
>
> **L-DEPLOY-005**: G4 代码评审必须包含编译构建通过和应用启动验证两个 blocker 项。
>
> **L-TEST-004**: 自动化测试全绿 ≠ 系统可用。集成测试使用 H2 + MockBean，与真实环境差异巨大，必须有人工测试环节。
>
> **L-TEST-005**: 人工测试必须以用户故事为单位，模拟真实用户操作流程，才能发现流程衔接中的问题。

这些经验来自真实的流程回退事件，确保同样的错误不会重复发生。

### 6. 多 Run 隔离

每次流程执行有独立的 `run_id`（如 `mvp-2026-06-24`），产出文档归档隔离：

```
docs/pipeline/
├── mvp-2026-06-24/          # MVP 版本流程产出
│   ├── docchat-prd.md
│   ├── docchat-technical-design.md
│   ├── docchat-code-review.md
│   └── ...
├── v1-2026-07-15/           # V1 版本流程产出（互不干扰）
└── latest -> mvp-2026-06-24/
```

---

## 📊 实验进展

当前状态：**MVP 流程已完成**，含人工测试及 G5.5 评审，全部环节通过。

```
[✅] 需求分析 → [✅] G1需求评审 → [✅] 技术方案设计 → [✅] G2技术方案评审
→ [✅] 详细设计 → [✅] G3详细设计评审 → [✅] 编码实现 → [✅] G4代码评审
→ [✅] 单元测试 → [✅] 集成测试 → [✅] 人工测试 → [✅] G5.5人工测试评审
→ [✅] G5测试评审 → [✅] 部署上线 → [✅] G6上线验证
```

### 关键事件

| 事件 | 说明 |
|------|------|
| G4 回退 → 补强 | 代码评审首次通过后发现检查清单缺少"编译构建通过"和"应用启动验证"两个 blocker 项，主动回退补强后重新评审通过 |
| 23/23 编码任务 | 编码环节按详细设计的任务拆分，23 个任务全部完成，产出 88 个 Java 源文件 |
| MVP 部署上线 | Docker Compose 全栈部署成功，上线验证通过 |
| 新增人工测试环节 | MVP 部署复盘后发现"自动化测试全绿≠系统可用"，新增人工测试环节（14 环节→7 卡点） |
| G5.5 人工测试评审通过 | 3 轮缺陷修复（8 个缺陷全部修复），G5.5 评审 4/4 blocker + 4/4 major 通过 |
| 19 条经验沉淀 | 流程执行中已积累 19 条工具选择与经验教训记录 |

---

## 🏗️ 产品架构（DocChat SaaS）

### 分层架构

```
Controller → Service → Repository → Entity
  ↓ DTO入     ↓ 业务逻辑  ↓ 数据访问   ↓ 纯数据
  ↓ @Valid     ↓ 跨模块调用 ↓ JPA+Milvus ↓ 无逻辑
```

**依赖方向严格单向**，禁止反向依赖。模块间调用通过 Service 层。

### 模块划分

```
module-tenant ← (被所有模块依赖)
module-knowledge → module-task (文档上传触发切分任务)
module-chat ← module-knowledge (对话时检索知识库)
module-widget ← module-chat (组件嵌入对话能力)
```

| 模块 | 职责 | 阶段 |
|------|------|------|
| module-tenant | 注册登录、租户、角色权限 | MVP |
| module-knowledge | 文档上传、切分向量化 | MVP |
| module-task | 异步任务状态、进度、重试 | MVP |
| module-chat | RAG 问答、向量检索、LLM | MVP |
| module-widget | 聊天组件配置、脚本生成、预览窗口 | MVP |
| module-apikey | API Key 生成/吊销、调用限制 | V1 |
| module-stat | 调用量、Token 统计 | V1 |
| module-eval | 问答对、Hit Rate 评测 | V1 |

---

## 🛠️ 技术栈

```
后端:   Java 21 + Spring Boot 3.3 + JPA + PostgreSQL 16 + Milvus 2.4 + Redis 7
前端:   TypeScript + Vue 3 + Ant Design Vue 4 + Pinia + Vite 6
组件:   TypeScript + Vite (lib mode, IIFE 输出)
工具:   Maven 3.9 + pnpm 9 + Flyway + Docker Compose
LLM:   讯飞 Coding Plan API
```

| 层面 | 选型 | 版本 | 用途 |
|------|------|------|------|
| 后端语言 | Java | 21+ | LTS，支持虚拟线程 |
| Web 框架 | Spring Boot | 3.3+ | 基于 Spring 6 |
| 关系数据库 | PostgreSQL | 16+ | 主存储，JSONB 灵活结构 |
| 向量数据库 | Milvus | 2.4+ | RAG 向量检索 |
| 缓存/队列 | Redis | 7.0+ | 缓存 + 异步任务队列 |
| 前端框架 | Vue 3 | 3.4+ | Composition API |
| UI 组件库 | Ant Design Vue | 4.x | 管理后台 UI |
| 构建工具 | Vite | 5.x | 前端 + 组件构建 |
| 数据库迁移 | Flyway | 随 Spring Boot | 版本化 DDL |

---

## 📁 项目结构

```
docchat/
├── .ai/                           # AI Agent 约束文件
│   ├── product.md                 # 产品定位和功能边界
│   ├── tech.md                    # 技术栈约定
│   ├── structure.md               # 目录结构和架构规范
│   └── codeRule.md                # 代码规范
├── .dev-pipeline/                 # 开发流程引擎
│   ├── phases.yaml                # 14 个流程环节定义
│   ├── gates.yaml                 # 7 个卡点门禁定义
│   ├── tools-and-lessons.yaml     # 工具选择和经验记录
│   ├── pipeline-state.yaml        # 流程状态跟踪
│   └── templates/                 # 各环节文档模板
├── .claude/                       # Claude Code 配置 + Skills
├── server/                        # 后端 — Spring Boot 3 + Java 21
│   └── src/main/java/com/docchat/
│       ├── common/                # 公共层（config/exception/response/util）
│       ├── module_tenant/         # 租户与用户管理
│       ├── module_knowledge/      # 知识库管理
│       ├── module_task/           # 异步任务处理
│       ├── module_chat/           # RAG 对话服务
│       └── module_widget/         # 聊天组件管理
├── web/                           # 管理后台 — Vue 3 + Ant Design Vue
├── packages/chat-widget/          # 嵌入式聊天组件 — TypeScript IIFE
├── docker/                        # Docker 配置（全栈编排）
├── docs/                          # 项目文档
│   ├── pipeline/                  # 流程产出归档（按 run_id 隔离）
│   └── ...
└── scripts/                       # 工具脚本
```

---

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.9+
- Node.js 18+ & pnpm 9+
- Docker & Docker Compose

### 启动基础设施

```bash
docker compose up -d    # PostgreSQL + Redis + Milvus(etcd+MinIO)
```

### 启动后端

```bash
cd server && mvn spring-boot:run
```

### 启动前端

```bash
cd web && pnpm install && pnpm dev
```

---

## 🔄 流程引擎使用

通过 `/dev-pipeline` 命令启动完整的瀑布开发流程：

```
/dev-pipeline              # 从头开始或从断点继续
/dev-pipeline status       # 查看当前流程状态
/dev-pipeline resume       # 从上次中断位置继续
/dev-pipeline phase=coding # 跳转到指定环节
```

### 配套 Skill

| Skill | 用途 | 时机 |
|-------|------|------|
| `/product` | 产品定位与价值设计 | 流程前 |
| `/architect` | 技术选型与架构设计 | 流程前 |
| `/build_doc_sys` | LLM 友好文档体系构建 | 流程前/中 |
| `/dev-pipeline` | 瀑布流程执行引擎 | 核心流程 |
| `/code-review` | 代码评审 | G4 环节 |
| `/verify` | 变更验证 | 部署/测试环节 |
| `/user-manual` | 用户手册生成（人工测试前置） | 人工测试环节 |
| `/retrospect` | 版本复盘 | G6 通过后 |
| `/refresh-docs` | 文档刷新 + commit-message 生成 | 每次提交前 |
| `/refactor-expert` | 架构偏差识别 + 零风险增量重构 | 流程完成后兜底对齐 |

---

## 🔑 关键规则

- **多租户隔离**：TenantContext + AOP，禁止手动传 tenant_id
- **统一响应**：`R<T>` 封装，错误码 `{MODULE}_{ERROR_TYPE}`
- **文件上传**：白名单(PDF/MD/TXT) + 50MB上限 + UUID重命名
- **JWT无状态鉴权**：管理后台用JWT，聊天组件用 widget_token
- **对话即焚**：不持久化终端访客对话

---

## 📋 里程碑

- [x] 产品定位与功能边界
- [x] 技术选型与架构设计
- [x] 项目骨架初始化
- [x] MVP：租户管理 + 知识库管理 + 异步任务 + RAG对话 + 聊天组件（上线验证通过，人工测试评审通过）
- [ ] V1：API Key + 用量统计 + 评测集 + 聊天组件预览与模拟测试 + LLM API配置

---

## 💡 实验启示（持续更新）

本项目验证了几个关键洞察：

1. **流程即代码** — 将开发流程定义为 YAML 配置（环节、卡点、检查清单），Agent 可像执行程序一样严格执行
2. **回退不是失败** — G4 代码评审的回退和补强，证明了卡点机制的价值：问题发现越早，修复成本越低
3. **约束文件是 Agent 的"宪法"** — 明确的规则边界让 Agent 行为可预测，避免"自由发挥"导致架构失控
4. **人类评审不可省略** — Agent 可以执行所有环节，但质量判断仍需人类把关
5. **经验沉淀防止重蹈覆辙** — 每次回退和修正都记录为经验，形成 Agent 的"组织记忆"
6. **自动化测试全绿 ≠ 系统可用** — 集成测试使用 H2 + MockBean 与真实环境差异巨大，人工测试以用户故事为单位验证端到端流程，是自动化测试的必要补充

---

## 许可证

[BSL 1.1](LICENSE)
