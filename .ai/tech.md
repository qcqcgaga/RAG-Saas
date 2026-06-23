# 技术栈约定

> 本文件定义项目的技术选型和版本约束，所有代码必须遵守此约定。

## 后端

| 层面 | 选型 | 版本约束 | 备注 |
|------|------|----------|------|
| 语言 | Java | 21+ | LTS 版本，支持虚拟线程 |
| 运行时 | JDK 21 (HotSpot) | 21+ | 可选 GraalVM 原生编译 |
| Web 框架 | Spring Boot | 3.3+ | 基于 Spring 6，要求 Java 17+ |
| ORM / 数据库访问 | Spring Data JPA | 随 Spring Boot 版本 | 配合 Hibernate 实现 |
| 数据库 | PostgreSQL | 16+ | 主数据存储，利用 JSONB 存储灵活结构 |
| 向量数据库 | Milvus | 2.4+ | RAG 向量检索，混合检索支持 |
| 缓存 / 任务队列 | Redis | 7.0+ | 缓存 + 异步任务队列（自建轻量队列） |
| 数据库迁移 | Flyway | 随 Spring Boot 版本 | 版本化管理 DDL 变更 |
| 认证 / 鉴权 | Spring Security + JWT | 随 Spring Boot 版本 | 无状态 Token 方案，适合 SaaS |
| API 风格 | REST | — | 标准 RESTful API，资源化路径 |
| LLM 接入 | 讯飞 Coding Plan API | — | 对话生成层，通过 HTTP 调用 |
| 包管理器 | Maven | 3.9+ | 约定优于配置，依赖管理稳定 |

## 前端

| 层面 | 选型 | 版本约束 | 备注 |
|------|------|----------|------|
| 语言 | TypeScript | 5.x | 类型安全，提升代码质量 |
| 框架 | Vue 3 | 3.4+ | Composition API + `<script setup>` |
| UI 组件库 | Ant Design Vue | 4.x | 设计现代，组件丰富，适合管理后台 |
| 状态管理 | Pinia | 2.x | Vue 3 官方推荐，轻量 |
| HTTP 客户端 | Axios | 1.x | 拦截器、请求/响应转换 |
| 构建工具 | Vite | 5.x | Vue 3 默认构建工具，开发体验好 |
| 包管理器 | pnpm | 9.x | 节省磁盘空间，严格依赖隔离 |
| 路由 | Vue Router | 4.x | Vue 3 官方路由 |

## 聊天组件（嵌入端）

| 层面 | 选型 | 版本约束 | 备注 |
|------|------|----------|------|
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建工具 | Vite (lib mode) | 5.x | 输出 IIFE 格式，可嵌入任意网页 |
| 样式方案 | 原生 CSS / CSS Modules | — | 隔离宿主页面样式，避免冲突 |
| 通信 | postMessage / fetch | — | 与父页面通信，直接调用后端 API |

## 开发工具链

| 工具 | 选型 | 版本约束 | 备注 |
|------|------|----------|------|
| 版本控制 | Git | 2.x | |
| 代码格式化（后端） | google-java-format | 1.x | Java 代码格式统一 |
| 代码格式化（前端） | Prettier | 3.x | Vue/TS/CSS 格式统一 |
| Lint（后端） | Checkstyle | 10.x | 配合 google-java-format 规则 |
| Lint（前端） | ESLint | 9.x | Vue + TS 规则集 |
| 测试框架（后端） | JUnit 5 + Mockito | 5.x / 5.x | 单元测试 + Mock |
| 测试框架（前端） | Vitest | 1.x | Vite 原生测试框架 |
| API 测试 | Spring MockMvc + TestContainers | — | 集成测试，真实数据库 |
| 容器化 | Docker + Docker Compose | 24+ / 2.x | 开发和生产环境统一 |

## 约束规则

- **版本锁定**：生产依赖必须锁定主版本号（major），Maven 使用 BOM 管理版本，pnpm 使用 lock 文件
- **最小依赖**：不引入功能重叠的库，优先使用框架内置能力
- **统一语言**：后端统一使用 Java，前端统一使用 TypeScript + Vue 3
- **变更流程**：技术栈变更需在此文件记录，并同步更新 `structure.md` 和 `codeRule.md`

## 决策记录

| 决策 | 选择 | 理由 | 权衡 | 日期 |
|------|------|------|------|------|
| 后端语言 | Java 21 | 团队熟悉、生态成熟、虚拟线程支持 | 放弃了 Python 的 AI 生态直连便利 | 2026-06-23 |
| 前端框架 | Vue 3 | 团队偏好、上手快、中文社区强 | 放弃了 React 的更大生态 | 2026-06-23 |
| ORM | Spring Data JPA | Spring 生态默认、开发效率高 | 复杂查询控制力弱于 MyBatis | 2026-06-23 |
| 向量数据库 | Milvus | 开源、功能全面、混合检索 | 运维成本高于云托管方案 | 2026-06-23 |
| 异步任务 | Redis + 自建队列 | 轻量、无额外中间件依赖 | 无 Dashboard，需自建监控 | 2026-06-23 |
| 关系数据库 | PostgreSQL | 功能强大、JSONB 支持 | 运维经验可能不如 MySQL 丰富 | 2026-06-23 |
| UI 组件库 | Ant Design Vue | 设计现代、组件丰富 | 包体比 Element Plus 稍大 | 2026-06-23 |
| LLM | 讯飞 Coding Plan API | 用户指定 | 依赖讯飞服务可用性 | 2026-06-23 |
