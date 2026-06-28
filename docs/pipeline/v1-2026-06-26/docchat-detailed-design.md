# 详细设计文档

> 项目：DocChat — 文档智能客服 SaaS
> 版本：V1
> 日期：2026-06-26

## 1. 模块概述

V1 在 MVP 基础上新增 3 个后端模块、变更 2 个后端模块、变更前端和聊天组件。各模块职责：

| 模块 | 职责 | 新增/变更 |
|------|------|----------|
| module-apikey | API Key 生成/吊销/限额/鉴权 | 新增 |
| module-stat | 用量采集(AOP)+统计查询+面板数据 | 新增 |
| module-eval | 评测集CRUD+评测执行(异步)+结果存储 | 新增 |
| module-chat | 双鉴权+统计切面+LLM租户配置 | 变更 |
| module-widget | 嵌入代码使用API Key | 变更 |
| web (前端) | 新增3个管理页面+Widget预览iframe | 变更 |
| chat-widget | postMessage监听+API Key鉴权+reset() | 变更 |

## 2. 类图

### 2.1 module-apikey

```mermaid
classDiagram
    class ApiKeyController {
        +createKey(req: CreateApiKeyRequest) R~ApiKeyResponse~
        +listKeys() R~ApiKeyListResponse~
        +revokeKey(keyId: Long, req: ConfirmRequest) R~ApiKeyResponse~
        +renameKey(keyId: Long, req: RenameKeyRequest) R~ApiKeyResponse~
    }

    class ApiKeyService {
        +createKey(tenantId: Long, name: String) ApiKeyResponse
        +listKeys(tenantId: Long) List~ApiKeyListVO~
        +revokeKey(tenantId: Long, keyId: Long) ApiKeyResponse
        +renameKey(tenantId: Long, keyId: Long, name: String) ApiKeyResponse
        +validateKey(keyHash: String) ApiKeyValidationResult
        +checkQuota(tenantId: Long) boolean
        +incrementQuota(tenantId: Long) void
    }

    class ApiKeyRepository {
        +findByKeyHash(keyHash: String) Optional~ApiKey~
        +findByTenantIdAndStatus(tenantId: Long, status: int) List~ApiKey~
        +countByTenantIdAndStatus(tenantId: Long, status: int) long
    }

    class ApiKey {
        +Long id
        +Long tenantId
        +String keyHash
        +String keyEncrypted
        +String keyPrefix
        +String name
        +int status
        +Instant lastUsedAt
        +Instant createdAt
        +Instant revokedAt
    }

    class ApiKeyValidationResult {
        +boolean valid
        +Long tenantId
        +Long apiKeyId
        +String authType
    }

    ApiKeyController --> ApiKeyService
    ApiKeyService --> ApiKeyRepository
    ApiKeyService --> ApiKey
    ApiKeyService --> ApiKeyValidationResult
```

### 2.2 module-stat

```mermaid
classDiagram
    class StatController {
        +getOverview(period: String) R~StatOverviewResponse~
        +getDaily(startDate: String, endDate: String) R~StatDailyListResponse~
        +getTrend(period: String, metric: String) R~StatTrendResponse~
    }

    class StatService {
        +getOverview(tenantId: Long, period: String) StatOverviewVO
        +getDaily(tenantId: Long, startDate: LocalDate, endDate: LocalDate) List~StatDailyVO~
        +getTrend(tenantId: Long, period: String, metric: String) StatTrendVO
        +recordUsage(tenantId: Long, apiKeyId: Long, authType: String, tokens: TokenUsage) void
    }

    class ChatUsageLogRepository {
        +sumByTenantAndDateRange(tenantId: Long, start: LocalDate, end: LocalDate) StatAggregation
        +findByTenantAndDate(tenantId: Long, date: LocalDate) List~ChatUsageLog~
    }

    class ChatUsageLog {
        +Long id
        +Long tenantId
        +Long apiKeyId
        +String authType
        +String modelName
        +int promptTokens
        +int completionTokens
        +int totalTokens
        +Instant createdAt
    }

    class ChatStatAspect {
        +aroundConverse(joinPoint: ProceedingJoinPoint) Object
    }

    StatController --> StatService
    StatService --> ChatUsageLogRepository
    StatService --> ChatUsageLog
    ChatStatAspect --> StatService
```

### 2.3 module-eval

```mermaid
classDiagram
    class EvalController {
        +createSet(req: CreateEvalSetRequest) R~EvalSetResponse~
        +listSets() R~EvalSetListResponse~
        +getSet(setId: Long) R~EvalSetDetailResponse~
        +updateSet(setId: Long, req: UpdateEvalSetRequest) R~EvalSetResponse~
        +deleteSet(setId: Long) R~Void~
        +addPair(setId: Long, req: AddEvalPairRequest) R~EvalPairResponse~
        +listPairs(setId: Long) R~EvalPairListResponse~
        +deletePair(setId: Long, pairId: Long) R~Void~
        +importPairs(setId: Long, req: ImportPairsRequest) R~ImportResult~
        +runEval(setId: Long) R~EvalRunResponse~
        +listResults(setId: Long) R~EvalResultListResponse~
        +getResult(setId: Long, resultId: Long) R~EvalResultDetailResponse~
    }

    class EvalService {
        +createSet(tenantId: Long, name: String, desc: String) EvalSetResponse
        +listSets(tenantId: Long) List~EvalSetVO~
        +getSet(tenantId: Long, setId: Long) EvalSetDetailVO
        +deleteSet(tenantId: Long, setId: Long) void
        +addPair(tenantId: Long, setId: Long, question: String, expectedDoc: String) EvalPairVO
        +importPairs(tenantId: Long, setId: Long, pairs: List) ImportResult
        +runEval(tenantId: Long, setId: Long) EvalRunResponse
        +getResult(tenantId: Long, setId: Long, resultId: Long) EvalResultDetailVO
    }

    class EvalSetRepository {
        +findByTenantId(tenantId: Long) List~EvalSet~
        +countByTenantId(tenantId: Long) long
    }

    class EvalPairRepository {
        +findByEvalSetId(setId: Long) List~EvalPair~
        +countByEvalSetId(setId: Long) long
    }

    class EvalResultRepository {
        +findByEvalSetId(setId: Long) List~EvalResult~
        +findLatestByEvalSetId(setId: Long) Optional~EvalResult~
    }

    EvalController --> EvalService
    EvalService --> EvalSetRepository
    EvalService --> EvalPairRepository
    EvalService --> EvalResultRepository
```

### 2.4 module-chat 变更

```mermaid
classDiagram
    class ChatController {
        +converse(authHeader: String, req: ChatRequest) SseEmitter
    }

    class ChatServiceImpl {
        +converse(req: ChatRequest, tenantId: Long) SseEmitter
    }

    class LlmService {
        +streamChat(prompt: String, tenantId: Long, tokenConsumer: Consumer) void
        +getTenantLlmConfig(tenantId: Long) TenantLlmConfig
        -callLlmApi(apiUrl: String, apiKey: String, modelName: String, prompt: String, consumer: Consumer) void
    }

    class ChatStatAspect {
        +aroundConverse(joinPoint: ProceedingJoinPoint) Object
        -determineAuthType() String
        -recordUsage(tenantId: Long, apiKeyId: Long, authType: String, tokens: TokenUsage) void
    }

    class AuthResolver {
        +resolve(authHeader: String) AuthResult
    }

    class AuthResult {
        +Long tenantId
        +Long apiKeyId
        +String authType
    }

    ChatController --> AuthResolver
    ChatController --> ChatServiceImpl
    ChatServiceImpl --> LlmService
    ChatStatAspect --> StatService
```

## 3. 时序图

### 3.1 API Key 鉴权对话流程

```mermaid
sequenceDiagram
    participant W as ChatWidget
    participant CC as ChatController
    participant AR as AuthResolver
    participant AKS as ApiKeyService
    participant CS as ChatService
    participant CSA as ChatStatAspect
    participant SS as StatService
    participant LS as LlmService

    W->>CC: POST /chat/conversations (Bearer dc_xxxx)
    CC->>AR: resolve("Bearer dc_xxxx")
    AR->>AR: 判断前缀 dc_ → API Key
    AR->>AKS: validateKey(keyHash)
    AKS->>AKS: Redis缓存查询/查库
    AKS-->>AR: AuthResult(tenantId=1, apiKeyId=5, authType=API_KEY)
    AR-->>CC: AuthResult

    CC->>AKS: checkQuota(tenantId=1)
    AKS->>AKS: Redis GET quota:1:20260626
    AKS-->>CC: 未超限

    CC->>CSA: AOP拦截 → aroundConverse
    CSA->>CS: converse(req, tenantId=1)
    CS->>LS: streamChat(prompt, tenantId=1, consumer)
    LS->>LS: getTenantLlmConfig(1) → 查租户配置/fallback系统默认
    LS-->>CS: SSE 流式返回
    CS-->>CSA: 返回结果

    CSA->>CSA: determineAuthType() → API_KEY
    CSA->>SS: recordUsage(tenantId=1, apiKeyId=5, authType=API_KEY, tokens)
    CSA->>AKS: incrementQuota(tenantId=1)
    CSA-->>CC: 返回SseEmitter
    CC-->>W: SSE 流式响应
```

### 3.2 JWT 预览对话流程

```mermaid
sequenceDiagram
    participant IF as iframe(预览窗口)
    participant CC as ChatController
    participant AR as AuthResolver
    participant CS as ChatService
    participant CSA as ChatStatAspect

    IF->>CC: POST /chat/conversations (Bearer eyJxxx)
    CC->>AR: resolve("Bearer eyJxxx")
    AR->>AR: 判断前缀 eyJ → JWT
    AR-->>CC: AuthResult(tenantId=1, authType=JWT)

    CC->>CSA: AOP拦截 → aroundConverse
    CSA->>CS: converse(req, tenantId=1)
    CS-->>CSA: SSE 流式返回

    CSA->>CSA: determineAuthType() → JWT → 跳过记录
    CSA-->>CC: 返回SseEmitter
    CC-->>IF: SSE 流式响应
```

### 3.3 评测执行流程

```mermaid
sequenceDiagram
    participant A as Admin(管理后台)
    participant ES as EvalService
    participant TQ as Redis Queue
    participant EW as EvalWorker
    participant RS as RetrievalService
    participant ER as EvalResultRepository

    A->>ES: POST /eval/sets/{setId}/run
    ES->>ES: 检查评测集非空+防重复执行锁
    ES->>TQ: 推送评测任务
    ES-->>A: {resultId, status=RUNNING}

    TQ->>EW: 消费评测任务
    EW->>ES: 查询所有问答对
    loop 对每个问答对
        EW->>RS: search(question, tenantId, topK=5)
        RS-->>EW: 检索结果列表
        EW->>EW: 检查期望文档是否在结果中
    end
    EW->>EW: 计算 Hit Rate = hitCount / totalPairs
    EW->>ER: 保存 EvalResult (detail_json)
    EW-->>TQ: 任务完成
```

### 3.4 超限拒绝流程

```mermaid
sequenceDiagram
    participant W as ChatWidget
    participant CC as ChatController
    participant AKS as ApiKeyService

    W->>CC: POST /chat/conversations (Bearer dc_xxxx)
    CC->>AKS: checkQuota(tenantId=1)
    AKS->>AKS: Redis GET quota:1:20260626 → 1000 (已达限额)
    AKS-->>CC: 超限

    CC-->>W: 429 Too Many Requests {code: 40804, msg: "APIKEY_QUOTA_EXCEEDED"}
```

## 4. 状态机

### 4.1 API Key 状态

```mermaid
stateDiagram-v2
    [*] --> Active: 创建
    Active --> Revoked: 吊销(不可逆)
    Revoked --> [*]
```

### 4.2 评测结果状态

```mermaid
stateDiagram-v2
    [*] --> Running: 触发评测
    Running --> Completed: 评测成功
    Running --> Failed: 评测失败(Milvus不可用等)
    Completed --> [*]
    Failed --> [*]
```

## 5. 关键算法

### 5.1 API Key 生成算法

**输入**：租户ID + 可选名称

**输出**：ApiKey 实体（含 keyHash、keyEncrypted、keyPrefix）

**算法步骤**：

1. 检查租户有效 Key 数量 ≤ 5
2. 生成随机 Key：`dc_` + 32位随机hex字符（`SecureRandom`）
3. 计算 keyHash = SHA-256(key) → 用于快速查找
4. 计算 keyEncrypted = AES-256-Encrypt(key, secretKey) → 用于存储原文
5. 计算 keyPrefix = key.substring(0, 7) → 用于脱敏展示
6. 保存到 api_keys 表
7. 返回完整 key（仅此一次）

### 5.2 每日限额计数算法

**输入**：tenantId

**输出**：是否超限

**算法步骤**：

1. 构造 Redis Key：`docchat:quota:{tenantId}:{yyyyMMdd}`
2. Redis INCR key → 获取当前计数
3. 如果 key 不存在（首次），设置 EXPIRE 到次日 UTC 零点
4. 比较计数与 `tenants.daily_chat_limit`
5. 超限 → 返回 false，未超限 → 返回 true

**复杂度**：O(1)（Redis INCR 原子操作）

### 5.3 评测 Hit Rate 计算算法

**输入**：评测集 ID + 租户 ID

**输出**：Hit Rate 百分比

**算法步骤**：

1. 查询评测集所有问答对
2. 对每个问答对：
   a. 使用 RetrievalService.search(question, tenantId, topK=5)
   b. 检查期望文档名是否出现在 Top-K 结果中
   c. 记录 hit=true/false + 实际检索结果列表
3. hitCount = 命中的问答对数
4. hitRate = hitCount / totalPairs × 100
5. 将结果保存到 eval_results 表（detail_json 为 JSONB）

**复杂度**：O(N) × 向量检索复杂度，N 为问答对数量

## 6. 错误处理策略

| 异常场景 | 错误码 | 处理方式 |
|----------|--------|---------|
| API Key 无效/已吊销 | APIKEY_INVALID (40802) | 返回 401，提示 Key 无效 |
| API Key 超限 | APIKEY_QUOTA_EXCEEDED (40804) | 返回 429，提示超限 |
| Key 数量超限 | APIKEY_LIMIT_EXCEEDED (40803) | 返回 400，提示最多 5 个 |
| 评测集超限 | EVAL_SET_LIMIT_EXCEEDED (41002) | 返回 400，提示最多 10 个 |
| 问答对超限 | EVAL_PAIR_LIMIT_EXCEEDED (41003) | 返回 400，提示最多 50 对 |
| 评测重复执行 | EVAL_ALREADY_RUNNING (41004) | 返回 409，提示正在执行 |
| LLM 连通失败 | LLM_CONFIG_TEST_FAILED (41102) | 返回 503，提示连通失败 |
| LLM 配置 URL 不合法 | LLM_CONFIG_URL_INVALID (41101) | 返回 400，提示 URL 不合法 |

## 7. 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-26 | V1 初始版本，定义 7 模块类图 + 4 个时序图 + 2 个状态机 + 3 个算法 |
