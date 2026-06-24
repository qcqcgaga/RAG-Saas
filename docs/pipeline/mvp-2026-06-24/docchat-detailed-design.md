# 详细设计文档

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24

## 1. 模块概述

MVP 阶段包含 5 个业务模块 + 1 个公共层，按依赖顺序实现：

| 顺序 | 模块 | 职责 | 依赖 |
|------|------|------|------|
| 1 | common | 全局配置、异常体系、响应封装、租户上下文 | 无 |
| 2 | module-tenant | 注册登录、租户管理、团队成员 | common |
| 3 | module-knowledge | 文档上传/删除、版本管理 | common, module-tenant, module-task |
| 4 | module-task | 异步任务提交/查询/重试、任务执行器 | common, module-tenant |
| 5 | module-chat | RAG 对话、向量检索、LLM 调用 | common, module-tenant, module-knowledge |
| 6 | module-widget | 组件配置、嵌入脚本生成 | common, module-tenant, module-chat |

## 2. 类图

### 2.1 公共层 (common)

```mermaid
classDiagram
    class R~T~ {
        +int code
        +String msg
        +T data
        +ok(data: T) R~T~
        +fail(errorCode: ErrorCode) R~T~
    }

    class PageResult~T~ {
        +List~T~ list
        +long total
        +int page
        +int size
    }

    class BizException {
        +ErrorCode errorCode
        +String detail
    }

    class SystemException {
        +ErrorCode errorCode
        +Throwable cause
    }

    class GlobalExceptionHandler {
        +handleBizException(e: BizException) R
        +handleSystemException(e: SystemException) R
        +handleValidationException(e: MethodArgumentNotValidException) R
    }

    class TenantContext {
        -ThreadLocal~Long~ tenantIdHolder
        +setTenantId(id: Long) void
        +getTenantId() Long
        +clear() void
    }

    class TenantFilter {
        +doFilter(request: HttpServletRequest, chain: FilterChain) void
    }

    class ErrorCode {
        +int code
        +String msg
    }
```

### 2.2 租户模块 (module-tenant)

```mermaid
classDiagram
    class AuthController {
        +register(req: RegisterRequest) R~AuthResponse~
        +login(req: LoginRequest) R~AuthResponse~
    }

    class TenantController {
        +getCurrentTenant() R~TenantResponse~
        +updateCurrentTenant(req: UpdateTenantRequest) R~TenantResponse~
        +listMembers(page: int, size: int) R~PageResult~
        +inviteMember(req: InviteMemberRequest) R~MemberResponse~
        +updateMemberRole(userId: Long, req: UpdateRoleRequest) R~Void~
        +removeMember(userId: Long) R~Void~
    }

    class AuthService {
        +register(req: RegisterRequest) AuthResponse
        +login(req: LoginRequest) AuthResponse
        -checkLoginFailCount(email: String) void
        -incrementLoginFailCount(email: String) void
        -resetLoginFailCount(email: String) void
    }

    class TenantService {
        +getCurrentTenant() TenantResponse
        +updateCurrentTenant(req: UpdateTenantRequest) TenantResponse
        +listMembers(pageable: Pageable) PageResult
        +inviteMember(req: InviteMemberRequest) MemberResponse
        +updateMemberRole(userId: Long, role: String) void
        +removeMember(userId: Long) void
    }

    class TenantRepository {
        <<interface JPA>>
    }

    class UserRepository {
        <<interface JPA>>
        +findByEmailAndTenantId(email: String, tenantId: Long) Optional~User~
        +existsByEmailAndTenantId(email: String, tenantId: Long) boolean
    }

    AuthController --> AuthService
    TenantController --> TenantService
    AuthService --> UserRepository
    AuthService --> TenantRepository
    TenantService --> TenantRepository
    TenantService --> UserRepository
```

### 2.3 知识库模块 (module-knowledge)

```mermaid
classDiagram
    class KnowledgeController {
        +getKnowledge() R~KnowledgeResponse~
        +updateKnowledge(req: UpdateKnowledgeRequest) R~KnowledgeResponse~
        +listDocuments(page: int, size: int, keyword: String, status: String) R~PageResult~
        +uploadDocument(file: MultipartFile, strategy: String, chunkSize: int, overlap: int) R~DocumentUploadResponse~
        +getDocument(documentId: Long) R~DocumentDetailResponse~
        +deleteDocument(documentId: Long, confirm: boolean) R~Void~
        +listVersions(documentId: Long) R~List~
        +rollbackVersion(documentId: Long, versionId: Long) R~Void~
    }

    class KnowledgeService {
        +getKnowledge() KnowledgeResponse
        +updateKnowledge(req: UpdateKnowledgeRequest) KnowledgeResponse
        +listDocuments(query: DocumentQuery) PageResult
        +uploadDocument(req: DocumentUploadRequest) DocumentUploadResponse
        +getDocument(documentId: Long) DocumentDetailResponse
        +deleteDocument(documentId: Long) void
        +listVersions(documentId: Long) List
        +rollbackVersion(documentId: Long, versionId: Long) void
    }

    class DocumentFileValidator {
        +validateFileType(file: MultipartFile) void
        +validateFileSize(file: MultipartFile) void
        +validateFileHeader(file: MultipartFile) void
        +generateStoredName(originalName: String) String
    }

    class KnowledgeRepository {
        <<interface JPA>>
    }

    class DocumentRepository {
        <<interface JPA>>
        +findByKnowledgeIdAndStatus(knowledgeId: Long, status: String, pageable: Pageable) Page
        +countByKnowledgeId(knowledgeId: Long) long
    }

    class DocumentVersionRepository {
        <<interface JPA>>
        +findMaxVersionNumberByDocumentId(documentId: Long) Integer
    }

    KnowledgeController --> KnowledgeService
    KnowledgeService --> KnowledgeRepository
    KnowledgeService --> DocumentRepository
    KnowledgeService --> DocumentVersionRepository
    KnowledgeService --> DocumentFileValidator
    KnowledgeService --> TaskService
```

### 2.4 任务模块 (module-task)

```mermaid
classDiagram
    class TaskController {
        +listTasks(page: int, size: int) R~PageResult~
        +getTask(taskId: Long) R~TaskDetailResponse~
        +retryTask(taskId: Long) R~TaskResponse~
    }

    class TaskService {
        +createTask(type: String, documentId: Long) AsyncTask
        +listTasks(pageable: Pageable) PageResult
        +getTask(taskId: Long) TaskDetailResponse
        +retryTask(taskId: Long) TaskResponse
        +updateTaskProgress(taskId: Long, progress: int) void
        +updateTaskStatus(taskId: Long, status: String, errorMsg: String) void
    }

    class TaskQueueService {
        +pushTask(task: AsyncTask) void
        +popTask(taskType: String) AsyncTask
        +acquireLock(taskId: Long) boolean
        +releaseLock(taskId: Long) void
    }

    class TaskWorker {
        +onTask(task: AsyncTask) void
        -processChunkAndEmbed(task: AsyncTask) void
        -processDeleteVectors(task: AsyncTask) void
    }

    class DocumentChunker {
        +chunkFixed(text: String, size: int, overlap: int) List~String~
        +chunkBySentence(text: String) List~String~
        +chunkByParagraph(text: String) List~String~
    }

    class EmbeddingService {
        +embed(texts: List~String~) List~float[]~
    }

    class TaskRepository {
        <<interface JPA>>
        +findByTenantIdAndStatus(tenantId: Long, status: String, pageable: Pageable) Page
    }

    TaskController --> TaskService
    TaskService --> TaskRepository
    TaskService --> TaskQueueService
    TaskWorker --> TaskQueueService
    TaskWorker --> DocumentChunker
    TaskWorker --> EmbeddingService
    TaskWorker --> TaskService
```

### 2.5 对话模块 (module-chat)

```mermaid
classDiagram
    class ChatController {
        +converse(req: ChatRequest, response: HttpServletResponse) void
    }

    class ChatService {
        +converse(question: String, tenantId: Long) Flux~ChatEvent~
        -buildPrompt(question: String, chunks: List~DocumentChunk~) String
        -buildSystemPrompt() String
    }

    class RetrievalService {
        +retrieve(question: String, tenantId: Long, topK: int) List~DocumentChunk~
        -embedQuery(question: String) float[]
    }

    class LlmService {
        +streamChat(prompt: String) Flux~String~
        -buildRequestBody(prompt: String) Object
    }

    class MilvusRepository {
        +insert(collection: String, data: List~Map~) void
        +search(collection: String, vector: float[], topK: int) List~SearchResult~
        +deleteByDocumentId(collection: String, documentId: Long) void
        +createCollection(name: String, dimension: int) void
    }

    ChatController --> ChatService
    ChatService --> RetrievalService
    ChatService --> LlmService
    RetrievalService --> MilvusRepository
    RetrievalService --> EmbeddingService
```

### 2.6 组件模块 (module-widget)

```mermaid
classDiagram
    class WidgetController {
        +getConfig(token: String) R~WidgetConfigResponse~
        +updateConfig(req: UpdateWidgetConfigRequest) R~WidgetConfigResponse~
        +getEmbedScript() R~EmbedScriptResponse~
        +regenerateToken() R~TokenResponse~
    }

    class WidgetService {
        +getConfigByToken(token: String) WidgetConfigResponse
        +updateConfig(req: UpdateWidgetConfigRequest) WidgetConfigResponse
        +getEmbedScript(tenantId: Long) EmbedScriptResponse
        +regenerateToken(tenantId: Long) TokenResponse
        -generateWidgetToken() String
        -buildEmbedScript(token: String) String
    }

    class WidgetConfigRepository {
        <<interface JPA>>
        +findByWidgetToken(token: String) Optional~WidgetConfig~
    }

    WidgetController --> WidgetService
    WidgetService --> WidgetConfigRepository
```

## 3. 时序图

### 3.1 用户注册流程

```mermaid
sequenceDiagram
    participant C as 客户端
    participant Ctrl as AuthController
    participant Svc as AuthService
    participant UserRepo as UserRepository
    participant TenantRepo as TenantRepository
    participant DB as PostgreSQL

    C->>Ctrl: POST /api/v1/auth/register
    Ctrl->>Ctrl: @Valid 校验请求参数
    alt 校验失败
        Ctrl-->>C: 400 参数校验失败
    end
    Ctrl->>Svc: register(req)
    Svc->>UserRepo: existsByEmailAndTenantId(email, *)
    UserRepo->>DB: SELECT EXISTS
    DB-->>UserRepo: false
    Svc->>TenantRepo: save(new Tenant)
    TenantRepo->>DB: INSERT INTO tenants
    DB-->>TenantRepo: tenant
    Svc->>UserRepo: save(new User with BCrypt(password))
    UserRepo->>DB: INSERT INTO users
    DB-->>UserRepo: user
    Svc->>Svc: generate JWT Token (userId + tenantId + role)
    Svc-->>Ctrl: AuthResponse
    Ctrl-->>C: 200 { token, userId, tenantId }
```

### 3.2 文档上传流程

```mermaid
sequenceDiagram
    participant C as 客户端
    participant Ctrl as KnowledgeController
    participant Svc as KnowledgeService
    participant Validator as DocumentFileValidator
    participant TaskSvc as TaskService
    participant DB as PostgreSQL
    participant Redis as Redis Queue

    C->>Ctrl: POST /api/v1/knowledge/documents (multipart)
    Ctrl->>Ctrl: @Valid 校验
    Ctrl->>Svc: uploadDocument(file, strategy, chunkSize, overlap)
    Svc->>Validator: validateFileType(file)
    Svc->>Validator: validateFileSize(file)
    Svc->>Validator: validateFileHeader(file)
    Svc->>Validator: generateStoredName(originalName)
    Svc->>DB: INSERT INTO knowledge_documents (status=PENDING)
    Svc->>DB: INSERT INTO document_versions (version=1)
    Svc->>TaskSvc: createTask(CHUNK_AND_EMBED, documentId)
    TaskSvc->>DB: INSERT INTO async_tasks (status=PENDING)
    TaskSvc->>Redis: LPUSH docchat:task:queue:CHUNK_AND_EMBED
    Svc-->>Ctrl: { documentId, taskId, status }
    Ctrl-->>C: 200 { documentId, taskId, status }
```

### 3.3 任务执行流程

```mermaid
sequenceDiagram
    participant Worker as TaskWorker
    participant Redis as Redis Queue
    participant TaskSvc as TaskService
    participant Chunker as DocumentChunker
    participant EmbedSvc as EmbeddingService
    participant Milvus as MilvusRepository
    participant DB as PostgreSQL

    Worker->>Redis: RPOP docchat:task:queue:CHUNK_AND_EMBED
    Redis-->>Worker: taskId
    Worker->>TaskSvc: updateTaskStatus(taskId, PROCESSING)
    TaskSvc->>DB: UPDATE async_tasks SET status='PROCESSING'

    Worker->>Chunker: chunkFixed(text, chunkSize, overlap)
    Chunker-->>Worker: List<chunks>

    loop 每批 chunks
        Worker->>EmbedSvc: embed(chunks)
        EmbedSvc-->>Worker: List<vectors>
        Worker->>Milvus: insert(collection, vectors)
        Worker->>TaskSvc: updateTaskProgress(taskId, progress%)
    end

    Worker->>DB: UPDATE knowledge_documents SET status='COMPLETED', chunk_count=N
    Worker->>TaskSvc: updateTaskStatus(taskId, COMPLETED)
    TaskSvc->>DB: UPDATE async_tasks SET status='COMPLETED'

    alt 执行失败
        Worker->>TaskSvc: updateTaskStatus(taskId, FAILED, errorMsg)
        TaskSvc->>DB: UPDATE async_tasks SET status='FAILED', error_message=?
    end
```

### 3.4 RAG 对话流程

```mermaid
sequenceDiagram
    participant Widget as Chat Widget
    participant Ctrl as ChatController
    participant Svc as ChatService
    participant Retrieval as RetrievalService
    participant Milvus as MilvusRepository
    participant LLM as LlmService
    participant Xunfei as 讯飞 API

    Widget->>Ctrl: POST /api/v1/chat/conversations (SSE)
    Ctrl->>Ctrl: 解析 widget_token → tenantId
    Ctrl->>Svc: converse(question, tenantId)

    Svc->>Retrieval: retrieve(question, tenantId, topK=5)
    Retrieval->>Retrieval: embedQuery(question) → queryVector
    Retrieval->>Milvus: search(collection, queryVector, topK=5)
    Milvus-->>Retrieval: List<SearchResult> (chunk + score)
    Retrieval-->>Svc: List<DocumentChunk>

    Svc->>Svc: buildPrompt(question, chunks)
    Svc->>LLM: streamChat(prompt)

    loop 流式返回
        LLM->>Xunfei: HTTP POST (stream=true)
        Xunfei-->>LLM: SSE token 片段
        LLM-->>Svc: Flux<String>
        Svc-->>Ctrl: ChatEvent(token)
        Ctrl-->>Widget: SSE event: token
    end

    Svc-->>Ctrl: ChatEvent(done, sources)
    Ctrl-->>Widget: SSE event: done + sources
```

### 3.5 任务重试异常流程

```mermaid
sequenceDiagram
    participant C as 客户端
    participant Ctrl as TaskController
    participant Svc as TaskService
    participant DB as PostgreSQL
    participant Redis as Redis Queue

    C->>Ctrl: POST /api/v1/tasks/{taskId}/retry
    Ctrl->>Svc: retryTask(taskId)
    Svc->>DB: SELECT async_tasks WHERE id=taskId
    alt 任务状态不是 FAILED
        Svc-->>Ctrl: 抛出 BizException(TASK_NOT_FAILED)
        Ctrl-->>C: 400 TASK_NOT_FAILED
    else retryCount >= maxRetry
        Svc-->>Ctrl: 抛出 BizException(TASK_MAX_RETRY_EXCEEDED)
        Ctrl-->>C: 400 TASK_MAX_RETRY_EXCEEDED
    else 可重试
        Svc->>DB: UPDATE async_tasks SET status='PENDING', retry_count=retry_count+1
        Svc->>Redis: LPUSH docchat:task:queue:{taskType}
        Svc-->>Ctrl: TaskResponse
        Ctrl-->>C: 200 { taskId, status, retryCount }
    end
```

## 4. 状态机

### 4.1 文档处理状态

```mermaid
stateDiagram-v2
    [*] --> PENDING: 上传成功
    PENDING --> PROCESSING: 任务开始执行
    PROCESSING --> COMPLETED: 切分+向量化完成
    PROCESSING --> FAILED: 执行出错
    FAILED --> PENDING: 重试
    COMPLETED --> PROCESSING: 版本回滚/重新切分
    COMPLETED --> [*]: 删除
```

### 4.2 异步任务状态

```mermaid
stateDiagram-v2
    [*] --> PENDING: 任务创建
    PENDING --> PROCESSING: Worker 拉取
    PROCESSING --> COMPLETED: 执行成功
    PROCESSING --> FAILED: 执行失败
    FAILED --> PENDING: 重试 (retryCount < maxRetry)
    FAILED --> [*]: 超过最大重试次数
    COMPLETED --> [*]
```

### 4.3 用户角色权限

```mermaid
stateDiagram-v2
    [*] --> ADMIN: 注册创建者
    ADMIN --> MEMBER: 角色变更
    ADMIN --> READONLY: 角色变更
    MEMBER --> ADMIN: 角色变更
    MEMBER --> READONLY: 角色变更
    READONLY --> ADMIN: 角色变更
    READONLY --> MEMBER: 角色变更
```

**权限矩阵**：

| 操作 | ADMIN | MEMBER | READONLY |
|------|-------|--------|----------|
| 租户信息管理 | ✅ | ❌ | ❌ |
| 成员管理 | ✅ | ❌ | ❌ |
| 文档上传/删除 | ✅ | ✅ | ❌ |
| 文档查看 | ✅ | ✅ | ✅ |
| 版本回滚 | ✅ | ✅ | ❌ |
| 任务重试 | ✅ | ✅ | ❌ |
| 组件配置 | ✅ | ❌ | ❌ |
| 组件查看 | ✅ | ✅ | ✅ |

## 5. 关键算法

### 5.1 文档固定大小切分算法

**输入**：文档全文 text，切分块大小 chunkSize，重叠字符数 overlap

**输出**：切分段落列表 List\<String\>

**算法步骤**：

1. 将文本按字符读取
2. 从位置 0 开始，取 chunkSize 个字符作为一个 chunk
3. 下一个 chunk 的起始位置 = 当前起始位置 + chunkSize - overlap
4. 重复步骤 2-3 直到文本末尾
5. 最后一个 chunk 如果长度 < chunkSize/2，合并到前一个 chunk
6. 对每个 chunk 做前后空白字符 trim

**复杂度**：O(n) / O(n)，n 为文本长度

### 5.2 Milvus 混合检索算法

**输入**：用户问题 question，租户 ID tenantId，返回数量 topK

**输出**：相关文档片段列表 List\<DocumentChunk\>

**算法步骤**：

1. 调用 EmbeddingService 将 question 转为向量 queryVector
2. 在 Milvus collection `docchat_vectors_{tenantId}` 中执行向量搜索
3. 搜索参数：metric_type=COSINE, params={"nprobe": 8}, limit=topK
4. 返回结果按相似度分数降序排列
5. 过滤相似度分数 < 0.5 的结果（阈值可配置）
6. 将 Milvus 结果映射为 DocumentChunk（含 documentName, chunkIndex, content, score）

**复杂度**：O(n * d) 向量检索，n 为 collection 中向量数，d 为维度

### 5.3 RAG Prompt 构造算法

**输入**：用户问题 question，检索结果 chunks，系统提示 systemPrompt

**输出**：完整 Prompt 字符串

**算法步骤**：

1. 构造系统提示：`你是 DocChat 智能客服，基于以下文档内容回答用户问题。如果文档中没有相关信息，请诚实回答"抱歉，文档中没有相关信息"。`
2. 拼接检索结果：每个 chunk 格式为 `[来源: {documentName} 第{chunkIndex}段]\n{content}\n`
3. 拼接用户问题：`用户问题：{question}`
4. 限制 Prompt 总长度 < 4000 字符（超出则截断较早的 chunk）

**复杂度**：O(k)，k 为检索返回的 chunk 数量

## 6. 错误处理策略

| 异常场景 | 错误码 | 处理方式 |
|----------|--------|---------|
| 邮箱已注册 | AUTH_EMAIL_EXISTS | 返回 409，提示邮箱已注册 |
| 登录失败 | AUTH_LOGIN_FAILED | 返回 401，累计失败次数 |
| 账户锁定 | AUTH_ACCOUNT_LOCKED | 返回 423，提示等待时间 |
| 文件类型不支持 | KNOWLEDGE_FILE_TYPE_NOT_ALLOWED | 返回 400，列出支持的类型 |
| 文件过大 | KNOWLEDGE_FILE_TOO_LARGE | 返回 400，提示大小限制 |
| 文件头不匹配 | KNOWLEDGE_FILE_HEADER_MISMATCH | 返回 400，提示文件可能损坏 |
| 文档不存在 | KNOWLEDGE_DOCUMENT_NOT_FOUND | 返回 404 |
| 任务未失败 | TASK_NOT_FAILED | 返回 400，只有失败任务可重试 |
| 超过最大重试 | TASK_MAX_RETRY_EXCEEDED | 返回 400 |
| Widget Token 无效 | WIDGET_TOKEN_INVALID | 返回 401 |
| Widget 已禁用 | CHAT_WIDGET_DISABLED | 返回 403 |
| LLM 服务不可用 | CHAT_LLM_UNAVAILABLE | 返回 503，提示稍后重试 |
| 跨租户访问 | FORBIDDEN | 返回 403，Hibernate Filter 自动拦截 |
| 参数校验失败 | INVALID_PARAMETER | 返回 400，列出校验失败字段 |

## 7. 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-24 | 初始版本，完成 5 模块类图 + 5 时序图 + 3 状态机 + 3 关键算法 |
