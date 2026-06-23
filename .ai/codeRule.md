# 代码规范

> 本文件定义项目的编码规则，所有代码提交必须遵守。

## 命名规范

### 通用

| 场景 | 风格 | 示例 |
|------|------|------|
| 文件名 | kebab-case | `user-service.ts` / `UserService.java` |
| 目录名 | kebab-case | `user-management/` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 环境变量 | UPPER_SNAKE_CASE | `DATABASE_URL` |

### 后端（Java / Spring Boot）

| 场景 | 风格 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserService` |
| 方法/函数 | camelCase | `findById` |
| 变量 | camelCase | `userId` |
| 包名 | 全小写 | `com.docchat.module-knowledge.service` |
| 数据库表名 | snake_case 复数 | `users`, `knowledge_documents` |
| 数据库列名 | snake_case | `created_at`, `tenant_id` |
| API 路径 | kebab-case 复数 | `/api/knowledge-documents`, `/api/tasks` |
| DTO 命名 | `{Entity}{Action}Request/Response` | `KnowledgeUploadRequest`, `TaskStatusResponse` |
| Service 接口 | `{Domain}Service` | `KnowledgeService`（接口） → `KnowledgeServiceImpl`（实现） |

### 前端（Vue 3 / TypeScript）

| 场景 | 风格 | 示例 |
|------|------|------|
| 组件文件名 | PascalCase | `KnowledgeList.vue` |
| 组件名 | PascalCase | `<KnowledgeList />` |
| 组合式函数 | useCamelCase | `useKnowledgeList` |
| 事件处理 | handleCamelCase | `handleSubmit` |
| CSS 类名 | kebab-case / BEM | `knowledge-list__item--active` |
| API 文件 | kebab-case（与后端模块名一致） | `knowledge.ts`, `task.ts` |
| Store 文件 | kebab-case（与后端模块名一致） | `knowledge.ts`, `user.ts` |
| 类型文件 | kebab-case.d.ts | `knowledge.d.ts` |

## 复杂度限制

| 指标 | 上限 | 说明 |
|------|------|------|
| 函数行数 | 50 行 | 超出则拆分 |
| 函数参数 | 4 个 | 超出则使用对象/DTO |
| 嵌套层级 | 3 层 | 超出则提前 return 或提取函数 |
| 文件行数 | 300 行 | 超出则拆分模块 |
| 圈复杂度 | 10 | 超出则简化逻辑 |
| 单个模块依赖数 | 8 个 | 超出则审视职责划分 |
| Controller 方法行数 | 20 行 | 只做参数校验、调用 Service、格式化响应 |

## 安全规则

### 输入校验

- 所有外部输入（HTTP 参数、Header、Body）必须在 Controller 层使用 `@Valid` / `@Validated` 校验
- Bean Validation 注解：`@NotNull`、`@Size`、`@Pattern`、`@Email` 等
- 自定义校验注解用于业务规则（如文件类型白名单校验）

### SQL 注入防护

- 禁止拼接 SQL，必须使用 JPA 参数化查询 / `@Query` 占位符
- 原生 SQL 仅在 JPA 无法满足时使用，必须使用参数绑定

### 多租户隔离

- **核心机制**：TenantContext + AOP 自动过滤
- 每个 API 请求通过拦截器解析租户 ID，写入 TenantContext（ThreadLocal）
- Repository 层通过 Hibernate Filter 自动注入 `tenant_id` 条件
- **禁止**：在 Service 层手动传递 tenant_id 做过滤（容易遗漏）
- **禁止**：跨租户查询（除非管理后台全局管理功能需要，且显式标注）

### 文件上传安全

- **文件类型白名单**：仅允许 PDF、Markdown、TXT
- **文件大小限制**：单文件 < 50MB
- **文件头校验**：读取文件前几个字节验证真实类型，防止伪装扩展名
- **文件名处理**：上传后使用 UUID 重命名，不使用原始文件名存储
- **存储隔离**：按 tenant_id 分目录存储

### XSS 防护

- 前端渲染用户内容必须转义，禁止 `v-html`（除非明确安全且标注原因）
- 后端返回的对话内容不包含 HTML 标签，使用纯文本

### 敏感数据

- 密码使用 BCrypt 哈希存储，禁止明文
- JWT Token 不入日志，不入数据库
- API Key 使用加密存储，展示时脱敏（只显示前 4 位和后 4 位）
- 租户 ID 不在 URL 路径中暴露（通过 Token 解析，不从请求参数获取）

### 错误信息安全

- 生产环境不暴露堆栈、SQL、内部路径、框架版本
- 全局异常处理器统一包装错误响应

### 依赖安全

- 定期运行 `mvn dependency-check:check` 审计后端依赖
- 定期运行 `pnpm audit` 审计前端依赖
- 不使用已知有漏洞的包

### 环境变量

- 密钥、连接串等通过环境变量注入，不硬编码
- `.env` 文件加入 `.gitignore`
- 配置文件中敏感值使用 `${ENV_VAR}` 占位符

## 错误处理

### 后端

- **统一异常体系**：
  - `BaseException` — 业务异常基类
  - `BizException` — 可预期的业务错误（如"文档不存在"）
  - `SystemException` — 系统级错误（如"数据库连接失败"）
- **全局异常处理器**：`@RestControllerAdvice` 统一捕获，返回标准 `R<T>` 响应
- **错误码**：结构化错误码 `{MODULE}_{ERROR_TYPE}`，如 `KNOWLEDGE_NOT_FOUND`、`TASK_PROCESSING_FAILED`
- **日志**：错误日志包含 Request-Id、错误码、关键参数（脱敏后）

### 前端

- **全局错误拦截**：Axios 响应拦截器统一处理 4xx/5xx
- **错误边界**：Vue `onErrorCaptured` 捕获组件渲染错误
- **用户提示**：使用 Ant Design Vue 的 `message.error()` 展示友好提示
- **禁止**：直接向用户展示后端原始错误信息

### 统一响应格式

```java
// 成功响应
R.ok(data)          → { "code": 0, "msg": "success", "data": {...} }

// 失败响应
R.fail(errorCode)   → { "code": 40401, "msg": "文档不存在", "data": null }

// 分页响应
R.ok(PageResult)    → { "code": 0, "msg": "success", "data": { "list": [...], "total": 100, "page": 1, "size": 20 } }
```

## 日志规范

- **框架**：SLF4J + Logback
- **链路追踪**：每个请求通过 MDC 注入 Request-Id，全链路日志关联
- **日志级别**：
  - ERROR：系统异常，需要人工介入
  - WARN：可预期的业务异常（如"文档格式不支持"）
  - INFO：关键业务操作（如"文档上传成功"、"对话开始"）
  - DEBUG：开发调试信息，生产环境不输出
- **脱敏规则**：日志中禁止输出密码、Token、API Key、完整手机号
- **格式**：`[Request-Id] [Tenant-Id] [Module] message`

## 测试规范

- **策略**：中等覆盖 — 核心 Service 单元测试 + 关键流程集成测试
- **单元测试**：JUnit 5 + Mockito，测试 Service 层业务逻辑
- **集成测试**：`@SpringBootTest` + TestContainers（PostgreSQL、Redis、Milvus）
- **命名约定**：`{被测类}Test.java`，测试方法名 `should_{预期结果}_when_{条件}`
- **覆盖率目标**：核心 Service > 80%，Controller > 50%（通过集成测试）
- **前端测试**：Vitest + Vue Test Utils，测试核心 hooks 和工具函数

## Git 规范

- **提交格式**：`type(scope): description`
  - type: feat / fix / docs / refactor / test / chore
  - scope: 模块名（tenant / knowledge / task / chat / widget / stat / apikey / eval / common）
  - 示例：`feat(knowledge): add document upload endpoint`
- **分支命名**：`type/brief-description`，如 `feat/knowledge-upload`
- **禁止提交**：`.env` 文件、`node_modules/`、IDE 配置、大文件、`logs/`

## 代码审查清单

- [ ] 命名符合规范
- [ ] 无硬编码密钥/连接串
- [ ] 输入已校验（@Valid）
- [ ] 多租户隔离正确（通过 TenantContext，非手动传 tenant_id）
- [ ] 错误已处理且不泄露内部信息
- [ ] 函数复杂度未超限
- [ ] 无重复代码（DRY）
- [ ] 新增依赖有必要且无安全风险
- [ ] 文件上传有类型/大小校验
- [ ] 日志不包含敏感信息

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-23 | 初始版本：基于 Spring Boot + Vue 3 技术栈完善命名、安全、错误处理、日志、测试规范 |
