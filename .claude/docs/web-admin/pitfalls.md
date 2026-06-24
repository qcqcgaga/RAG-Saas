# 管理后台坑点

## 1. Token 持久化

user store 中 token 仅存于内存（Pinia state）。
页面刷新后丢失，需跳转登录。
后续可改为 localStorage 持久化（需权衡 XSS 风险）。

## 2. 分页组件与后端对齐

后端 PageResult 字段: `{ list, total, page, size }`
Ant Design Vue Table 分页: `{ current, pageSize, total }`
注意 page 从 1 开始 vs current 从 1 开始的一致性。

## 3. 文件上传进度

知识库文档上传使用 multipart/form-data。
当前未展示上传进度条，大文件(50MB)体验差。
建议使用 Axios onUploadProgress 回调。

## 4. SSE 对话调试

ChatController 返回 text/event-stream。
前端使用 EventSource 消费，但 POST 请求不支持 EventSource。
需使用 fetch + ReadableStream 或 eventsource-polyfill。
