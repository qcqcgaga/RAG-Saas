/**
 * API Key 与访问控制模块 (module-apikey)
 *
 * 职责：API Key 生成/吊销/限额/鉴权
 *
 * 内部结构：
 * - controller/ : REST API 入口，API Key CRUD + 限额配置
 * - service/    : 核心业务逻辑，Key生成/吊销/验证/限额检查
 * - repository/ : JPA 数据访问
 * - entity/     : ApiKey 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant (获取租户上下文)
 * - 被 module-chat 依赖 (鉴权校验)
 * - 被 module-widget 依赖 (嵌入代码生成)
 */
package com.docchat.module_apikey;
