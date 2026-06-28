# 架构图

> 项目：DocChat — 文档智能客服 SaaS
> 版本：V1
> 日期：2026-06-26

## 1. 系统上下文图（V1 增量）

```mermaid
graph TB
    subgraph 用户
        Admin[租户管理员]
        Member[团队成员]
        Visitor[终端访客]
    end

    subgraph DocChat V1
        Backend[后端服务]
        Frontend[管理后台]
        Widget[聊天组件]
    end

    subgraph 外部服务
        LLM[LLM API 租户自配/系统默认]
    end

    Admin --> Frontend
    Member --> Frontend
    Visitor --> Widget
    Frontend --> Backend
    Widget --> Backend
    Backend --> LLM

    style LLM fill:#ff9,stroke:#333
```

## 2. 容器图

```mermaid
graph TB
    subgraph 前端
        Web[管理后台 Vue3<br/>Nginx :80]
        CW[聊天组件<br/>CDN 分发 IIFE]
    end

    subgraph 后端
        API[Spring Boot API<br/>:8080]
    end

    subgraph 数据层
        PG[(PostgreSQL :5432)]
        Milvus[(Milvus :19530)]
        Redis[(Redis :6379)]
    end

    subgraph 存储
        Uploads[文件存储<br/>./uploads]
    end

    Web --> API
    CW --> API
    API --> PG
    API --> Milvus
    API --> Redis
    API --> Uploads

    style Redis fill:#ff9,stroke:#333
```

## 3. V1 模块组件图

```mermaid
graph TB
    subgraph module-apikey "🔑 API Key 模块 (V1)"
        AKC[ApiKeyController]
        AKS[ApiKeyService]
        AKR[ApiKeyRepository]
        AKE[ApiKey Entity]
    end

    subgraph module-stat "📊 统计模块 (V1)"
        STC[StatController]
        STS[StatService]
        STR[StatRepository]
        STE[ChatUsageLog Entity]
    end

    subgraph module-eval "📝 评测模块 (V1)"
        EVC[EvalController]
        EVS[EvalService]
        EVR[EvalRepository]
        EVE[EvalSet / EvalPair / EvalResult Entity]
    end

    subgraph module-chat "对话模块 (V1 变更)"
        CHC[ChatController<br/>JWT + API Key 双鉴权]
        CHS[ChatService]
        CSAP[ChatStatAspect<br/>AOP 用量采集]
        LLM_S[LlmService<br/>租户级 LLM 配置]
    end

    subgraph module-widget "组件模块 (V1 变更)"
        WGC[WidgetController]
        WGS[WidgetService<br/>嵌入代码用 API Key]
    end

    subgraph chat-widget "聊天组件 (V1 变更)"
        CWI[ChatWidget<br/>postMessage + reset]
    end

    AKC --> AKS --> AKR --> AKE
    STC --> STS --> STR --> STE
    EVC --> EVS --> EVR --> EVE
    CHC --> CHS
    CSAP -.->|AOP 拦截| CHS
    CHS --> LLM_S

    WGC --> WGS

    CHS -.->|鉴权| AKS
    CSAP -.->|写入| STS
    EVS -.->|检索| module-knowledge

    style module-apikey fill:#fff3cd,stroke:#856404
    style module-stat fill:#d4edda,stroke:#155724
    style module-eval fill:#cce5ff,stroke:#004085
    style CSAP fill:#f8d7da,stroke:#721c24
    style LLM_S fill:#f8d7da,stroke:#721c24
```

## 4. V1 鉴权流程图

```mermaid
flowchart TD
    A[请求: POST /api/v1/chat/conversations] --> B{Authorization Header}
    B -->|dc_xxxx| C[API Key 鉴权]
    B -->|eyJ...| D[JWT 鉴权]

    C --> E{Redis 查询 Key 状态}
    E -->|Key 有效| F{检查每日限额}
    E -->|Key 无效| G[返回 401]
    F -->|未超限| H[设置鉴权类型=API_KEY]
    F -->|已超限| I[返回 429]

    D --> J{JWT 解析}
    J -->|有效| K[设置鉴权类型=JWT]
    J -->|无效| L[返回 401]

    H --> M[ChatService.converse]
    K --> M

    M --> N{ChatStatAspect}
    N -->|API_KEY| O[记录用量到 chat_usage_logs]
    N -->|JWT| P[跳过记录]
```

## 5. V1 预览对话架构

```mermaid
flowchart LR
    subgraph 管理后台
        WV[WidgetView.vue]
        IFRAME[iframe]
    end

    subgraph ChatWidget
        CW[ChatWidget.ts]
        PM[postMessage 监听器]
    end

    subgraph 后端
        CC[ChatController]
        CS[ChatService]
    end

    WV -->|1. 加载 ChatWidget| IFRAME
    IFRAME -->|2. 渲染| CW
    WV -->|3. config-update<br/>postMessage| PM
    WV -->|4. reset<br/>postMessage| PM
    PM -->|5. 热更新外观/重置| CW

    CW -->|6. 发起对话<br/>JWT 鉴权| CC
    CC --> CS
    CS -->|7. SSE 流式返回| CW

    style IFRAME fill:#e7f3ff,stroke:#333
    style PM fill:#fff3cd,stroke:#333
```

## 架构说明

### V1 新增模块

1. **module-apikey**：独立模块，被 module-chat 依赖（鉴权校验）。提供 API Key 的完整生命周期管理 + 每日调用限额（Redis 计数器）
2. **module-stat**：独立模块，通过 AOP（ChatStatAspect）与 module-chat 解耦。用量数据异步写入，统计查询按日聚合
3. **module-eval**：独立模块，依赖 module-knowledge（检索能力）和 module-task（异步执行框架）

### V1 变更模块

4. **module-chat**：核心变更是"双鉴权 + 统计采集 + LLM 租户配置"。ChatController 根据 token 前缀判断鉴权方式；ChatStatAspect 通过 AOP 拦截决定是否记录用量；LlmService 查租户配置表 fallback 到系统默认
5. **module-widget**：嵌入代码生成使用 API Key 替代 widget_token
6. **ChatWidget**：新增 postMessage 监听器支持管理后台的实时预览交互

### 关键设计决策

- **AOP 解耦统计**：ChatStatAspect 不修改 ChatService 任何代码，零侵入地实现用量采集
- **Redis 计数器做限额**：利用 Redis INCR + EXPIRE 实现高性能的每日调用计数，无需额外中间件
- **评测复用任务框架**：评测执行复用 module-task 的 Redis 队列 + Worker 机制，不重复造轮子
