/**
 * 评测集模块 (module-eval) — V1 阶段
 *
 * 职责：内置问答对管理、自动评测检索 Hit Rate、评测结果历史对比
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理评测集管理/执行评测请求
 * - service/    : 核心业务逻辑，评测执行、结果对比
 * - repository/ : JPA 数据访问
 * - entity/     : EvalSet、EvalQuestion、EvalResult 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-knowledge（评测知识库检索质量）
 */
package com.docchat.module_eval;
