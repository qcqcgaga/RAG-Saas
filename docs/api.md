# API 接口文档

> 本文档描述 DocChat 后端 REST API 接口规范。

## 通用规范

### 基础路径

```
POST /api/{module}/{resource}
```

### 统一响应格式

```json
// 成功
{ "code": 0, "msg": "success", "data": {...} }

// 失败
{ "code": 40401, "msg": "文档不存在", "data": null }

// 分页
{ "code": 0, "msg": "success", "data": { "list": [...], "total": 100, "page": 1, "size": 20 } }
```

### 认证方式

- 管理后台 API：JWT Bearer Token（`Authorization: Bearer xxx`）
- 聊天组件 API：API Key（`X-API-Key: xxx`）

## 模块接口

### 租户管理 /api/tenant

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/tenant/register | 注册（创建租户） |
| POST | /api/tenant/login | 登录 |
| GET | /api/tenant/me | 获取当前用户信息 |

### 知识库 /api/knowledge-documents

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/knowledge-documents | 文档列表（分页） |
| POST | /api/knowledge-documents | 上传文档（multipart） |
| DELETE | /api/knowledge-documents/{id} | 删除文档 |

### 任务 /api/tasks

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/tasks/{id} | 查询任务状态 |
| POST | /api/tasks/{id}/retry | 重试失败任务 |

### 对话 /api/chat

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat | 发送对话 |

### 聊天组件 /api/widgets

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/widgets/config | 获取组件配置 |
| PUT | /api/widgets/config | 更新组件配置 |
| GET | /api/widgets/script | 获取嵌入脚本 |
