# API 接口设计

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24

## 1. 接口概述

| 维度 | 说明 |
|------|------|
| 基础路径 | /api/v1 |
| 认证方式 | Bearer Token (JWT) / Widget Token |
| 响应格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 (yyyy-MM-dd'T'HH:mm:ss'Z') |

## 2. 统一响应格式

### 成功响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

### 错误响应

```json
{
  "code": 40401,
  "msg": "文档不存在",
  "data": null
}
```

### 分页响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

## 3. 接口列表

### 3.1 认证模块 (/api/v1/auth)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 1 | POST | /api/v1/auth/register | 用户注册 | 无 |
| 2 | POST | /api/v1/auth/login | 用户登录 | 无 |

### 3.2 租户模块 (/api/v1/tenants)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 3 | GET | /api/v1/tenants/current | 获取当前租户信息 | JWT |
| 4 | PUT | /api/v1/tenants/current | 更新当前租户信息 | JWT (ADMIN) |
| 5 | GET | /api/v1/tenants/members | 获取团队成员列表 | JWT |
| 6 | POST | /api/v1/tenants/members | 邀请团队成员 | JWT (ADMIN) |
| 7 | PUT | /api/v1/tenants/members/{userId}/role | 修改成员角色 | JWT (ADMIN) |
| 8 | DELETE | /api/v1/tenants/members/{userId} | 移除团队成员 | JWT (ADMIN) |

### 3.3 知识库模块 (/api/v1/knowledge)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 9 | GET | /api/v1/knowledge | 获取知识库信息 | JWT |
| 10 | PUT | /api/v1/knowledge | 更新知识库信息 | JWT (ADMIN/MEMBER) |
| 11 | GET | /api/v1/knowledge/documents | 获取文档列表（分页） | JWT |
| 12 | POST | /api/v1/knowledge/documents | 上传文档 | JWT (ADMIN/MEMBER) |
| 13 | GET | /api/v1/knowledge/documents/{documentId} | 获取文档详情 | JWT |
| 14 | DELETE | /api/v1/knowledge/documents/{documentId} | 删除文档 | JWT (ADMIN/MEMBER) |
| 15 | GET | /api/v1/knowledge/documents/{documentId}/versions | 获取文档版本列表 | JWT |
| 16 | POST | /api/v1/knowledge/documents/{documentId}/versions/{versionId}/rollback | 版本回滚 | JWT (ADMIN/MEMBER) |

### 3.4 异步任务模块 (/api/v1/tasks)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 17 | GET | /api/v1/tasks | 获取任务列表（分页） | JWT |
| 18 | GET | /api/v1/tasks/{taskId} | 获取任务详情 | JWT |
| 19 | POST | /api/v1/tasks/{taskId}/retry | 重试失败任务 | JWT (ADMIN/MEMBER) |

### 3.5 对话模块 (/api/v1/chat)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 20 | POST | /api/v1/chat/conversations | 发起对话（SSE 流式） | Widget Token |

### 3.6 聊天组件模块 (/api/v1/widget)

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 21 | GET | /api/v1/widget/config | 获取组件配置 | Widget Token |
| 22 | PUT | /api/v1/widget/config | 更新组件配置 | JWT (ADMIN) |
| 23 | GET | /api/v1/widget/embed-script | 获取嵌入脚本 | JWT |
| 24 | POST | /api/v1/widget/regenerate-token | 重新生成 Widget Token | JWT (ADMIN) |

## 4. 接口详细设计

### 4.1 POST /api/v1/auth/register — 用户注册

**请求**：

```json
{
  "email": "user@example.com",
  "password": "MyPass123",
  "tenantName": "我的团队"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| email | String | 是 | @Email 格式 | 邮箱 |
| password | String | 是 | >=8位，含字母和数字 | 密码 |
| tenantName | String | 是 | 1-100字符 | 租户名称 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "userId": 1,
    "tenantId": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### 4.2 POST /api/v1/auth/login — 用户登录

**请求**：

```json
{
  "email": "user@example.com",
  "password": "MyPass123"
}
```

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| email | String | 是 | 邮箱 |
| password | String | 是 | 密码 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "userId": 1,
    "tenantId": 1,
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| AUTH_LOGIN_FAILED | 401 | 邮箱或密码错误 |
| AUTH_ACCOUNT_LOCKED | 423 | 账户已锁定，请30分钟后重试 |
| AUTH_ACCOUNT_DISABLED | 403 | 账户已禁用 |

### 4.3 GET /api/v1/tenants/current — 获取当前租户信息

**请求头**：`Authorization: Bearer <token>`

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "我的团队",
    "slug": "my-team",
    "status": 1,
    "memberCount": 3,
    "documentCount": 15,
    "createdAt": "2026-06-24T10:00:00Z"
  }
}
```

### 4.4 POST /api/v1/tenants/members — 邀请团队成员

**请求**：

```json
{
  "email": "member@example.com",
  "role": "MEMBER"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| email | String | 是 | @Email 格式 | 被邀请人邮箱 |
| role | String | 是 | ADMIN/MEMBER/READONLY | 角色 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "userId": 2,
    "email": "member@example.com",
    "role": "MEMBER",
    "status": 1
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| MEMBER_ALREADY_EXISTS | 409 | 该邮箱已是团队成员 |
| MEMBER_INVITE_FORBIDDEN | 403 | 无权邀请成员 |

### 4.5 GET /api/v1/knowledge/documents — 获取文档列表

**请求**：

```
GET /api/v1/knowledge/documents?page=1&size=20&keyword=产品&status=COMPLETED
```

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页条数（最大100） |
| keyword | String | 否 | | 搜索关键词（文档名模糊匹配） |
| status | String | 否 | | 状态过滤：PENDING/PROCESSING/COMPLETED/FAILED |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "originalName": "产品手册.pdf",
        "fileType": "PDF",
        "fileSize": 5242880,
        "status": "COMPLETED",
        "chunkCount": 128,
        "latestTaskId": 10,
        "createdAt": "2026-06-24T10:00:00Z",
        "updatedAt": "2026-06-24T10:05:00Z"
      }
    ],
    "total": 15,
    "page": 1,
    "size": 20
  }
}
```

### 4.6 POST /api/v1/knowledge/documents — 上传文档

**请求**：`multipart/form-data`

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| file | File | 是 | 文件（PDF/MD/TXT，< 50MB） |
| chunkingStrategy | String | 否 | 切分策略，默认 FIXED_SIZE |
| chunkSize | Integer | 否 | 切分块大小，默认 500 |
| chunkOverlap | Integer | 否 | 切分重叠，默认 50 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "documentId": 1,
    "taskId": 10,
    "status": "PENDING"
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| KNOWLEDGE_FILE_TYPE_NOT_ALLOWED | 400 | 不支持的文件类型 |
| KNOWLEDGE_FILE_TOO_LARGE | 400 | 文件大小超过限制 |
| KNOWLEDGE_FILE_HEADER_MISMATCH | 400 | 文件头与扩展名不匹配 |
| KNOWLEDGE_DOCUMENT_LIMIT_EXCEEDED | 400 | 文档数量超过限制（100） |

### 4.7 DELETE /api/v1/knowledge/documents/{documentId} — 删除文档

**请求**：

```
DELETE /api/v1/knowledge/documents/1
```

**请求体**（二次确认）：

```json
{
  "confirm": true
}
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

**说明**：删除文档同时触发异步任务清理 Milvus 中的向量数据。

### 4.8 GET /api/v1/tasks/{taskId} — 获取任务详情

**请求**：

```
GET /api/v1/tasks/10
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10,
    "documentId": 1,
    "documentName": "产品手册.pdf",
    "taskType": "CHUNK_AND_EMBED",
    "status": "PROCESSING",
    "progress": 65,
    "retryCount": 0,
    "maxRetry": 3,
    "errorMessage": null,
    "startedAt": "2026-06-24T10:01:00Z",
    "completedAt": null,
    "createdAt": "2026-06-24T10:00:30Z"
  }
}
```

### 4.9 POST /api/v1/tasks/{taskId}/retry — 重试失败任务

**请求**：

```
POST /api/v1/tasks/10/retry
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10,
    "status": "PENDING",
    "retryCount": 1,
    "maxRetry": 3
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| TASK_NOT_FAILED | 400 | 任务未失败，无法重试 |
| TASK_MAX_RETRY_EXCEEDED | 400 | 已达最大重试次数 |

### 4.10 POST /api/v1/chat/conversations — 发起对话

**请求头**：`Authorization: Bearer <widget_token>`

**请求**：

```json
{
  "question": "如何重置密码？"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| question | String | 是 | 1-500字符 | 用户问题 |

**响应**：SSE 流式

```
event: token
data: {"content": "您"}

event: token
data: {"content": "可以"}

event: token
data: {"content": "通过以下步骤"}

event: done
data: {"sources": [{"documentName": "用户手册.pdf", "chunkIndex": 5, "content": "点击登录页面的\"忘记密码\"..."}]}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| CHAT_QUESTION_EMPTY | 400 | 问题不能为空 |
| CHAT_QUESTION_TOO_LONG | 400 | 问题超过500字符 |
| CHAT_WIDGET_DISABLED | 403 | 聊天组件已禁用 |
| CHAT_LLM_UNAVAILABLE | 503 | LLM 服务暂时不可用 |

### 4.11 PUT /api/v1/widget/config — 更新组件配置

**请求**：

```json
{
  "brandColor": "#1890ff",
  "welcomeMessage": "你好，有什么可以帮你的？",
  "iconUrl": "https://example.com/icon.png",
  "enabled": true
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| brandColor | String | 否 | HEX 颜色格式 | 品牌色 |
| welcomeMessage | String | 否 | 1-200字符 | 欢迎语 |
| iconUrl | String | 否 | 合法 URL | 图标 URL |
| enabled | Boolean | 否 | | 是否启用 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "brandColor": "#1890ff",
    "welcomeMessage": "你好，有什么可以帮你的？",
    "iconUrl": "https://example.com/icon.png",
    "enabled": true,
    "updatedAt": "2026-06-24T10:00:00Z"
  }
}
```

### 4.12 GET /api/v1/widget/embed-script — 获取嵌入脚本

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "script": "<script src=\"https://cdn.docchat.com/widget.js\" data-token=\"abc123\"></script>",
    "previewUrl": "https://preview.docchat.com/abc123"
  }
}
```

## 5. 错误码定义

### 5.1 通用错误码

| 错误码 | HTTP状态码 | 描述 |
|--------|-----------|------|
| 0 | 200 | 成功 |
| 40000 | 400 | 参数校验失败 |
| 40100 | 401 | 未认证 |
| 40300 | 403 | 无权限 |
| 40400 | 404 | 资源不存在 |
| 40900 | 409 | 资源冲突 |
| 50000 | 500 | 服务器内部错误 |

### 5.2 模块错误码

| 模块 | 错误码范围 | 示例 |
|------|-----------|------|
| AUTH | 401xx | AUTH_LOGIN_FAILED=40101, AUTH_ACCOUNT_LOCKED=40102 |
| MEMBER | 403xx | MEMBER_ALREADY_EXISTS=40301, MEMBER_INVITE_FORBIDDEN=40302 |
| KNOWLEDGE | 404xx | KNOWLEDGE_FILE_TYPE_NOT_ALLOWED=40401, KNOWLEDGE_FILE_TOO_LARGE=40402 |
| TASK | 405xx | TASK_NOT_FAILED=40501, TASK_MAX_RETRY_EXCEEDED=40502 |
| CHAT | 406xx | CHAT_QUESTION_EMPTY=40601, CHAT_LLM_UNAVAILABLE=40603 |
| WIDGET | 407xx | WIDGET_TOKEN_INVALID=40701 |

## 6. 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-24 | 初始版本，定义 24 个 API 接口 + 错误码体系 |
