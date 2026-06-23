/**
 * API Key 与访问控制模块 (module-apikey) — V1 阶段
 *
 * 职责：API Key 生成/吊销、每日调用次数硬限制
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理 API Key 管理请求
 * - service/    : 核心业务逻辑，Key 生成、吊销、用量检查
 * - repository/ : JPA 数据访问
 * - entity/     : ApiKey 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文，API Key 绑定租户）
 */
package com.docchat.module_apikey;
