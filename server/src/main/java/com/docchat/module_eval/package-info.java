/**
 * 评测模块 (module-eval)
 *
 * 职责：评测集 CRUD + 评测执行（异步）+ 结果存储
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理评测集和评测结果请求
 * - service/    : 核心业务逻辑，评测集管理、评测执行、结果统计
 * - repository/ : JPA 数据访问
 * - entity/     : EvalSet、EvalPair、EvalResult 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-chat（RetrievalService 向量检索能力）
 */
package com.docchat.module_eval;
