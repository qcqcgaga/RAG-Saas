# 异步任务模块坑点

## 1. TaskWorker 单实例限制

@Scheduled 轮询适用于 MVP 单实例部署。
多实例场景需要分布式锁（已有 acquireLock/releaseLock 基于 Redis SET NX）。
但多实例轮询同一队列可能导致空 pop，V1 需优化为 pub/sub 模式。

## 2. 任务超时无处理

当前没有任务超时机制。如果任务卡在 PROCESSING 状态：
- 不会自动恢复
- 需要手动调用 `POST /{taskId}/retry`
- V1 需增加超时检测（如 PROCESSING 超过 30 分钟自动标记 FAILED）

## 3. Redis 队列持久化

Redis List 作为任务队列，Redis 重启会丢失未处理任务。
生产环境需开启 Redis AOF 持久化，或改用 Redis Stream。

## 4. 进度更新粒度

TaskWorker 中进度值为手动设置（10/30/40/70/80/90/100），
不代表精确百分比，仅用于前端进度条展示。

## 5. 跨模块依赖

TaskWorker 直接依赖 module-knowledge 的 DocumentRepository、MilvusRepository、DocumentChunker、DocumentParser、EmbeddingService。
这是通过 Spring Bean 注入实现的，违反了"模块间通过 Service 调用"的规范。
V1 应重构为 TaskWorker 通过 KnowledgeService 接口调用。
