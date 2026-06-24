# 异步任务模块 (module-task)

> 异步任务提交、状态查询、失败重试。基于 Redis 队列 + @Scheduled 轮询。

## 目录结构

```
module-task/
├── config/TaskConfig.java            # 任务相关配置
├── controller/TaskController.java    # REST API
├── dto/TaskResponse.java, TaskDetailResponse.java
├── entity/AsyncTask.java
├── repository/AsyncTaskRepository.java
├── service/
│   ├── TaskService.java / TaskServiceImpl.java  # 任务CRUD+状态更新
│   └── TaskQueueService.java                    # Redis队列操作+分布式锁
└── worker/TaskWorker.java            # @Scheduled 轮询+执行
```

## API — TaskController

基础路径: `/api/v1/tasks`（需 JWT）

| 方法 | 路径 | 说明 | 响应 |
|------|------|------|------|
| GET | `/` | 任务列表(分页) | `R<PageResult<TaskResponse>>` |
| GET | `/{taskId}` | 任务详情 | `R<TaskDetailResponse>` |
| POST | `/{taskId}/retry` | 重试失败任务 | `R<TaskResponse>` |

## 数据模型 — async_tasks 表

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | 租户 |
| document_id | BIGINT | 关联文档 |
| task_type | VARCHAR(30) | CHUNK_AND_EMBED / DELETE_VECTORS |
| status | VARCHAR(20) | PENDING/PROCESSING/COMPLETED/FAILED |
| progress | SMALLINT | 0-100 |
| max_retry | SMALLINT, DEFAULT 3 | 最大重试次数 |
| retry_count | SMALLINT, DEFAULT 0 | 已重试次数 |
| error_message | TEXT | 失败原因 |
| started_at / completed_at / created_at / updated_at | TIMESTAMPTZ | |

## 核心机制

### 任务队列流程

```
KnowledgeService → TaskQueueService.pushTask(task) → Redis List
                                                        ↓
TaskWorker.pollTasks() (@Scheduled 5s) ← Redis List pop
     ↓
acquireLock(taskId) → Redis SET NX (分布式锁)
     ↓
processTask() → updateStatus(PROCESSING) → onTask() → updateStatus(COMPLETED/FAILED)
     ↓
releaseLock(taskId)
```

### 任务类型

| 类型 | 说明 | 执行者 |
|------|------|--------|
| CHUNK_AND_EMBED | 文档切分+向量化 | TaskWorker |
| DELETE_VECTORS | 删除文档向量 | TaskWorker |

## 详细文档

- [data-model.md](data-model.md)
- [pitfalls.md](pitfalls.md)
