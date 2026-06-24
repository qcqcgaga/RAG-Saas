# 聊天组件坑点

## 1. SSE POST 请求

浏览器原生 EventSource 仅支持 GET 请求。
ChatController 对话接口是 POST，需使用 fetch + ReadableStream 或第三方库。
当前实现可能需调整。

## 2. 跨域问题

组件嵌入第三方网站，调用 DocChat 后端 API。
需配置 CORS：
- `Access-Control-Allow-Origin: *` 或指定域名
- SecurityConfig 已放行 `/api/chat/**` 和 `/api/widget/**`

## 3. IIFE 格式兼容性

Vite lib mode 输出 IIFE，需确保：
- 不使用 ES Module 语法（import/export）
- 所有依赖打包进单文件
- 全局命名空间污染最小化

## 4. 移动端适配

组件需响应式：
- 桌面端：右下角浮动窗口
- 移动端：全屏覆盖

## 5. widget_token 在前端暴露

嵌入脚本中 `data-token` 暴露在 HTML 源码中。
任何查看页面源码的人都能获取 token 并调用 API。
V1 需增加：域名白名单验证、调用次数限制。
