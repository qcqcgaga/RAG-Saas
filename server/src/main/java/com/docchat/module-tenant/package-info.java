/**
 * 租户与用户管理模块 (module-tenant)
 *
 * 职责：注册登录、租户工作空间、团队成员邀请、角色权限（管理员/成员/只读）
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理注册/登录/租户管理/成员邀请请求
 * - service/    : 核心业务逻辑，用户认证、租户管理、角色权限
 * - repository/ : JPA 数据访问
 * - entity/     : User、Tenant、TenantMember 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 被所有模块依赖（提供租户上下文 TenantContext）
 * - 不依赖任何业务模块
 */
package com.docchat.module_tenant;
