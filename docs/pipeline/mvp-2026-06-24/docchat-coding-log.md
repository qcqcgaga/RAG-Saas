# 编码日志

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24

## 1. 编码总览

### 后端 (88 个 Java 源文件)

| 模块 | 文件数 | 关键文件 |
|------|--------|----------|
| common (公共层) | ~10 | R.java, ErrorCode.java, BizException.java, GlobalExceptionHandler.java, TenantContext.java, TenantFilter.java, JwtUtil.java, SecurityConfig.java, SecurityUtil.java |
| module-tenant | ~10 | AuthController.java, AuthServiceImpl.java, TenantController.java, TenantServiceImpl.java, User.java, Tenant.java |
| module-knowledge | ~15 | KnowledgeController.java, KnowledgeServiceImpl.java, DocumentFileValidator.java, DocumentChunker.java, EmbeddingService.java, DocumentParser.java, MilvusRepository.java, MilvusConfig.java |
| module-task | ~10 | TaskController.java, TaskServiceImpl.java, TaskQueueService.java, TaskWorker.java, AsyncTask.java |
| module-chat | ~8 | ChatController.java, ChatServiceImpl.java, RetrievalService.java, LlmService.java, SseEmitter 流式 |
| module-widget | ~8 | WidgetController.java, WidgetServiceImpl.java, WidgetConfig.java |

### 前端 (Vue 3 + TypeScript)

| 文件 | 功能 |
|------|------|
| LoginView.vue | 登录/注册双 Tab 页面 |
| TenantView.vue | 租户信息 + 成员管理 |
| KnowledgeView.vue | 知识库管理 + 文档上传/删除 + 版本展开行 |
| TaskView.vue | 任务列表 + 进度条 + 重试 |
| WidgetView.vue | 组件配置 + 实时预览 + 嵌入脚本复制 |
| AppLayout.vue | 侧边栏导航 + 头部租户信息 |

### 聊天组件 (IIFE)

| 文件 | 功能 |
|------|------|
| ChatWidget.ts | 完整组件类：配置加载、SSE 流式、来源引用 |
| widget.css | 隔离样式、响应式、动画 |
| widget.js (5.90 KB) | 构建产物 |

### Docker

| 文件 | 功能 |
|------|------|
| docker-compose.yml | PG + Redis + Milvus(etcd+MinIO) + Server + Web |
| Dockerfile.server | JDK 21 JRE 镜像 |
| Dockerfile.web | Node 构建 + Nginx 镜像 |
| nginx.conf | SPA 路由 + API 反向代理 + SSE 支持 |

## 2. 关键决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| SSE 实现 | SseEmitter (非 Flux) | 项目使用 MVC 模式，不能同时用 WebFlux |
| Milvus 客户端 | SDK v2.4.4 ConnectConfig | 正确的 v2 API 调用方式 |
| Embedding | 随机向量占位 | MVP 阶段讯飞 Embedding 端点待确认 |
| LLM 调用 | 占位模拟流式 | 讯飞 Coding Plan API 接入后续实现 |
| Widget 鉴权 | 简化解析 tenantId | MVP 阶段简化，后续实现完整 Widget Token 验证 |
| 登录失败锁定 | Redis 计数 5次/30min | 符合 PRD 验收标准 |
| 版本回滚安全修复 | 校验版本归属 + 新建版本号 | 修复了原始实现中的租户隔离漏洞 |
| 包名映射 | module-xxx → module_xxx | Java 包名不允许连字符 |

## 3. 技术债务

| 项目 | 优先级 | 说明 |
|------|--------|------|
| Embedding 替换 | P0 | 需接入真实 Embedding API（讯飞或第三方） |
| LLM 流式调用 | P0 | 需接入讯飞 Coding Plan API |
| Widget Token 完整验证 | P1 | 当前简化从 Header 解析 tenantId |
| PDF 大文件内存 | P2 | 大 PDF 解析可能 OOM，需流式处理 |
| Milvus collection 自动创建 | P2 | TaskWorker 中每次切分都检查，可优化为首次创建后缓存 |
| 前端 E2E 测试 | P2 | Vitest 单元测试待补充 |

## 4. 编码规范检查

- ✅ 函数行数 < 50（Controller 方法 < 20）
- ✅ 嵌套层级 < 3
- ✅ 参数个数 < 4
- ✅ Controller 只做参数校验 + 调用 Service + 格式化响应
- ✅ 统一响应格式 R<T>
- ✅ 全局异常处理器统一捕获
- ✅ 数字错误码体系（AUTH 401xx, KNOWLEDGE 404xx 等）
- ✅ TenantContext + Hibernate Filter 多租户隔离
- ✅ JWT Token 不入日志
- ✅ 文件上传类型白名单 + 大小限制 + 文件头校验 + UUID 重命名
- ✅ 密码 BCrypt 哈希
- ✅ CSS 类名 .docchat- 前缀隔离

## 5. 编译验证

- 后端: `mvn compile` — **BUILD SUCCESS** (88 源文件, 0 错误)
- 前端: `npx vue-tsc --noEmit` — **无错误**
- Widget: `vite build` — **成功** (widget.js 5.90KB + widget.css 3.84KB)

## 6. Flyway 迁移脚本

| 版本 | 内容 |
|------|------|
| V1 | tenants + users + tenant_members 表 |
| V2 | knowledge_documents + knowledge_versions + tasks 表 |
| V3 | 完善数据模型：调整约束、新增列、重命名表/列、创建 widget_configs |
| V4 | 修复问题：删除废弃 title 列、knowledge_id NOT NULL、file_type VARCHAR(10) |
| R__seed | 开发环境种子数据（测试租户+用户+知识库+Widget配置） |

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-24 | MVP 全量编码完成（23 个任务），后端 88 源文件编译通过，前端类型检查通过 |
