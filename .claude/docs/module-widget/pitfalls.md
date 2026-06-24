# 聊天组件模块坑点

## 1. widget_token 安全性

当前 widget_token 生成后永久有效，无过期机制。
V1 需增加：token 过期时间、手动吊销、访问次数限制。

## 2. 组件配置公开接口

`GET /config?token=xxx` 不需 JWT，通过 widget_token 验证。
需确保此接口只返回前端渲染所需的最少信息，不暴露租户敏感数据。

## 3. 嵌入脚本跨域

chat-widget 使用 fetch 调用后端 API，需配置 CORS。
SecurityConfig 中已放行 `/api/chat/**` 和 `/api/widget/**`。

## 4. 每租户一个组件配置

当前设计每租户只能有一个 WidgetConfig。
如需多组件实例（如不同页面不同样式），需修改为多配置模型。
