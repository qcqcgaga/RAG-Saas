/**
 * RAG 对话服务模块 (module-chat)
 *
 * 职责：基于知识库的问答对话，返回带来源引用的回答，对话即焚不持久化
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理对话请求
 * - service/    : 核心业务逻辑
 *   - ChatService.java      : 对话编排（检索 + 生成）
 *   - RetrievalService.java : 向量检索
 *   - LlmService.java       : LLM 调用抽象（讯飞 Coding Plan API）
 * - repository/ : Milvus 向量检索封装
 * - entity/     : 对话相关数据结构
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-knowledge（对话时检索知识库）
 * - 被 module-widget 依赖（组件嵌入对话能力）
 * - 被 module-stat 依赖（统计对话用量）
 */
package com.docchat.module_chat;
