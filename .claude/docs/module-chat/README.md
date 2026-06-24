# RAG 对话模块 (module-chat)

> 基于知识库的问答对话，SSE 流式返回，对话即焚不持久化

## 目录结构

```
module_chat/
├── controller/ChatController.java    # SSE 流式对话接口
├── dto/
│   ├── ChatRequest.java              # { question: String }
│   ├── ChatEvent.java                # { type: token/done/error, content, sources }
│   └── SourceReference.java          # { documentName, chunkIndex, content, score }
└── service/
    ├── ChatService.java / ChatServiceImpl.java  # 对话编排
    ├── RetrievalService.java         # 向量检索
    └── LlmService.java              # LLM 流式调用(当前占位)
```

## API — ChatController

基础路径: `/api/v1/chat`（**不需JWT**，用 widget_token）

| 方法 | 路径 | 说明 | 请求头 | 响应 |
|------|------|------|--------|------|
| POST | `/conversations` | 发起对话(SSE) | `Authorization: Bearer {tenantId}` | `text/event-stream` |

### SSE 事件类型

| 事件名 | 数据 | 说明 |
|--------|------|------|
| `token` | `ChatEvent{type:"token", content:"..."}` | 流式 token |
| `done` | `ChatEvent{type:"done", sources:[...]}` | 完成+来源引用 |
| `error` | `ChatEvent{type:"error", content:"..."}` | 错误 |

## RAG 对话流程

```
用户提问 → RetrievalService.retrieve(question, tenantId, topK=5)
        → EmbeddingService.embed(question)  // 向量化
        → MilvusRepository.search(collection, vector, topK)  // 检索
        → 过滤 score >= 0.5 的结果
        → 构建 Prompt (SYSTEM_PROMPT + 来源内容 + 用户问题, 截断4000字符)
        → LlmService.streamChat(prompt, tokenConsumer)  // 虚拟线程中执行
        → 逐 token 推送 SSE 事件
        → 完成后推送 done 事件(含来源引用)
```

## 关键配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| TOP_K | 5 | 检索返回的文档片段数 |
| MIN_SCORE | 0.5 | 最低相似度阈值 |
| MAX_PROMPT_LENGTH | 4000 | Prompt 最大字符数 |
| SSE_TIMEOUT_MS | 120000 | SSE 超时2分钟 |

## 详细文档

- [pitfalls.md](pitfalls.md)
