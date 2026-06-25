/**
 * 异步任务处理模块 (module-task)
 *
 * 职责：异步任务提交、状态查询、进度跟踪、失败重试
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理任务状态查询请求
 * - service/    : 核心业务逻辑，任务调度、Redis 队列管理、状态更新
 * - repository/ : JPA 数据访问
 * - entity/     : Task、TaskLog 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 被 module-knowledge 依赖（文档上传后触发切分向量化任务）
 */
package com.docchat.module_task;
