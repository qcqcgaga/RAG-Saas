/**
 * 用量统计模块 (module-stat)
 *
 * 职责：
 * - 用量数据采集（AOP 切面调用 recordUsage）
 * - 聚合查询（按日期/周期统计调用量、Token 消耗、会话数）
 * - 仪表盘数据输出（概览、日统计、趋势）
 *
 * 内部包结构：
 * - controller/  REST 接口
 * - service/     业务逻辑
 * - repository/  数据访问
 * - entity/      JPA 实体
 * - dto/         数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（租户隔离）
 * - 被 module-chat 依赖（AOP 切面调用 StatService.recordUsage）
 */
package com.docchat.module_stat;
