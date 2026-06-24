# RAG 对话模块坑点

## 1. LlmService 是占位实现

当前返回硬编码的模拟文本，逐3字符推送。
生产替换为讯飞 Coding Plan API 真实调用。
接口签名 `streamChat(String prompt, Consumer<String> tokenConsumer)` 不变。

## 2. widget_token 认证是 MVP 简化

当前 `parseTenantIdFromToken` 直接从 `Authorization: Bearer {tenantId}` 提取。
**这是安全漏洞**，任何知道 tenantId 的人都能调用。
V1 需实现真实 widget_token 验证机制。

## 3. SSE 超时处理

SseEmitter 超时 2 分钟，长对话可能超时。
虚拟线程中执行对话，超时后 emitter.completeWithError() 触发。

## 4. Prompt 截断可能丢失关键信息

MAX_PROMPT_LENGTH=4000 字符，来源内容过多时会被截断。
可能需要更智能的截断策略（如优先保留高分来源）。

## 5. 对话即焚

终端访客对话不持久化，问完即焚。
这意味着无法支持"基于上下文的追问"，每次对话独立。

## 6. EmbeddingService 跨模块调用

RetrievalService 直接注入 EmbeddingService（module-knowledge 的 Bean），
跨模块依赖通过 Spring 注入实现，非 Service 接口调用。
