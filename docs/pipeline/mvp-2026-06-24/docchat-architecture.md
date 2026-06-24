# 架构图

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24

## 1. 系统上下文图

```mermaid
graph TB
    System[DocChat 系统]

    Admin[租户管理员<br/>管理后台操作]
    Member[团队成员<br/>知识库协作]
    Visitor[终端访客<br/>聊天组件提问]
    LLM[讯飞 Coding Plan API<br/>LLM 对话生成]

    Admin -->|管理后台| System
    Member -->|管理后台| System
    Visitor -->|嵌入JS组件| System
    System -->|HTTP API| LLM
```

## 2. 容器图（Docker Compose 部署）

```mermaid
graph TB
    subgraph 客户端
        Browser[浏览器<br/>Vue 3 管理后台]
        Widget[Chat Widget<br/>IIFE 嵌入脚本]
    end

    subgraph Docker Compose
        Nginx[Nginx<br/>静态资源 + 反向代理]
        Server[Spring Boot<br/>JDK 21 后端服务]
        PG[(PostgreSQL 16<br/>主数据存储)]
        Redis[(Redis 7<br/>缓存 + 任务队列)]
        Milvus[Milvus 2.4<br/>向量检索]
        Etcd[etcd<br/>Milvus 元数据]
        MinIO[MinIO<br/>Milvus 存储]
    end

    subgraph 外部服务
        LLM[讯飞 Coding Plan API]
    end

    Browser -->|HTTP/HTTPS| Nginx
    Widget -->|fetch API| Nginx
    Nginx -->|反向代理 /api/*| Server
    Nginx -->|静态资源| Browser
    Server -->|JPA| PG
    Server -->|RedisTemplate| Redis
    Server -->|MilvusClient| Milvus
    Server -->|HTTP| LLM
    Milvus --> Etcd
    Milvus --> MinIO
```

## 3. 组件图（Spring Boot 内部）

```mermaid
graph TB
    subgraph Spring Boot 后端
        subgraph 公共层 common/
            Security[Security 配置<br/>JWT 认证 + RBAC]
            TenantFilter[TenantFilter<br/>租户上下文拦截器]
            GlobalExceptionHandler[全局异常处理器]
            UnifiedResponse[统一响应封装 R‹T›]
        end

        subgraph module-tenant
            TenantController[租户 API]
            TenantService[租户业务逻辑]
            TenantRepository[租户数据访问]
        end

        subgraph module-knowledge
            KnowledgeController[知识库 API]
            KnowledgeService[知识库业务逻辑]
            KnowledgeRepository[知识库数据访问]
        end

        subgraph module-task
            TaskController[任务 API]
            TaskService[任务业务逻辑]
            TaskQueue[Redis 任务队列]
            TaskWorker[任务执行器<br/>切分+向量化]
        end

        subgraph module-chat
            ChatController[对话 API<br/>SSE 流式]
            ChatService[对话编排]
            RetrievalService[向量检索]
            LlmService[LLM 调用]
        end

        subgraph module-widget
            WidgetController[组件 API]
            WidgetService[组件业务逻辑]
        end
    end

    subgraph 数据层
        PG[(PostgreSQL)]
        Milvus[(Milvus)]
        Redis[(Redis)]
    end

    subgraph 外部
        LLM[讯飞 API]
    end

    TenantController --> TenantService --> TenantRepository --> PG
    KnowledgeController --> KnowledgeService --> KnowledgeRepository --> PG
    KnowledgeService --> TaskService
    TaskService --> TaskQueue --> Redis
    TaskWorker --> TaskQueue
    TaskWorker --> Milvus
    ChatController --> ChatService
    ChatService --> RetrievalService --> Milvus
    ChatService --> LlmService --> LLM
    WidgetController --> WidgetService

    TenantFilter --> TenantRepository
```

## 4. 模块依赖图

```mermaid
graph TB
    Tenant[module-tenant<br/>租户与用户管理]
    Knowledge[module-knowledge<br/>知识库管理]
    Task[module-task<br/>异步任务处理]
    Chat[module-chat<br/>RAG 对话服务]
    Widget[module-widget<br/>聊天组件管理]

    Knowledge -->|依赖| Tenant
    Task -->|依赖| Tenant
    Knowledge -->|触发任务| Task
    Chat -->|检索知识库| Knowledge
    Chat -->|依赖| Tenant
    Widget -->|依赖| Chat
    Widget -->|依赖| Tenant

    style Tenant fill:#4CAF50,color:#fff
    style Knowledge fill:#2196F3,color:#fff
    style Task fill:#FF9800,color:#fff
    style Chat fill:#9C27B0,color:#fff
    style Widget fill:#F44336,color:#fff
```

## 5. 请求流程图

### 5.1 文档上传流程

```mermaid
sequenceDiagram
    participant Admin as 管理员
    participant API as Spring Boot
    participant PG as PostgreSQL
    participant Redis as Redis Queue
    participant Worker as Task Worker
    participant Milvus as Milvus

    Admin->>API: POST /api/v1/knowledge/documents (上传文件)
    API->>API: 校验文件类型+大小+文件头
    API->>PG: 保存文档记录 (status=PENDING)
    API->>Redis: 推入切分任务队列
    API-->>Admin: 返回 taskId

    loop 前端轮询
        Admin->>API: GET /api/v1/tasks/{taskId}
        API->>PG: 查询任务状态
        API-->>Admin: 返回任务状态+进度
    end

    Worker->>Redis: 拉取任务
    Worker->>PG: 更新任务状态 PROCESSING
    Worker->>Worker: 文档切分 (Chunking)
    Worker->>Worker: 文本向量化 (Embedding)
    Worker->>Milvus: 写入向量数据 (按 tenant_id 隔离 collection)
    Worker->>PG: 更新任务状态 COMPLETED
```

### 5.2 RAG 对话流程

```mermaid
sequenceDiagram
    participant Visitor as 终端访客
    participant Widget as Chat Widget
    participant API as Spring Boot
    participant Milvus as Milvus
    participant LLM as 讯飞 API

    Visitor->>Widget: 输入问题
    Widget->>API: POST /api/v1/chat/conversations (SSE)
    API->>API: 解析 widget_token 获取 tenant_id
    API->>Milvus: 向量检索 Top-K 相关文档片段
    Milvus-->>API: 返回相关片段 + 相似度分数
    API->>API: 构造 Prompt (系统提示 + 检索片段 + 用户问题)
    API->>LLM: 流式调用 LLM API
    loop 流式返回
        LLM-->>API: 返回 Token 片段
        API-->>Widget: SSE 推送 Token 片段
        Widget-->>Visitor: 实时展示回答
    end
    API-->>Widget: SSE 结束 + 来源引用
    Widget-->>Visitor: 展示来源引用
```

## 架构说明

1. **单体模块化**：MVP 阶段采用 Spring Boot 单体应用，通过包结构划分模块边界，为未来微服务拆分预留空间
2. **异步任务**：文档切分+向量化是 CPU/IO 密集型操作，通过 Redis 队列异步执行，不阻塞上传接口
3. **多租户隔离**：共享数据库 + Hibernate Filter 方案，平衡开发效率和隔离性；Milvus 按租户独立 collection 实现向量数据物理隔离
4. **SSE 流式**：对话接口使用 SSE 流式返回，提升用户体验（首 Token < 1s）
5. **组件嵌入**：Chat Widget 以 IIFE 格式输出，通过 `postMessage` 与宿主页面通信，样式使用 CSS Modules 隔离
