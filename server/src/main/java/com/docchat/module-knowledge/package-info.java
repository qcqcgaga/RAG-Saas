/**
 * 知识库管理模块 (module-knowledge)
 *
 * 职责：文档上传/删除、自动切分向量化、版本记录与回滚
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理文档上传/删除/查询请求
 * - service/    : 核心业务逻辑，切分策略、版本管理、触发任务
 * - repository/ : JPA 数据访问 + Milvus 向量操作封装
 * - entity/     : Knowledge、KnowledgeVersion 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-task（文档上传后触发切分向量化任务）
 * - 被 module-chat 依赖（对话时检索知识库）
 * - 被 module-eval 依赖（评测知识库检索质量）
 */
package com.docchat.module_knowledge;
