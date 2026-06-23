/**
 * 用量统计模块 (module-stat) — V1 阶段
 *
 * 职责：API 调用量、Token 消耗、对话数统计，按租户维度汇总
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理用量查询请求
 * - service/    : 核心业务逻辑，用量聚合、统计报表
 * - repository/ : JPA 数据访问
 * - entity/     : UsageRecord、DailyStatistic 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-chat（统计对话用量）
 */
package com.docchat.module_stat;
