/**
 * 聊天组件管理模块 (module-widget)
 *
 * 职责：聊天组件配置、JS 脚本生成、外观设置（品牌色、欢迎语、图标）
 *
 * 内部结构：
 * - controller/ : REST API 入口，处理组件配置/脚本获取请求
 * - service/    : 核心业务逻辑，配置管理、JS 脚本渲染
 * - repository/ : JPA 数据访问
 * - entity/     : WidgetConfig 实体
 * - dto/        : 请求/响应数据传输对象
 *
 * 依赖关系：
 * - 依赖 module-tenant（获取租户上下文）
 * - 依赖 module-chat（组件嵌入对话能力）
 */
package com.docchat.module_widget;
