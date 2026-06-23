# API 接口设计

> 项目：{项目名称}
> 日期：{YYYY-MM-DD}

## 1. 接口概述

| 维度 | 说明 |
|------|------|
| 基础路径 | /api/v1 |
| 认证方式 | {Bearer Token / Session / None} |
| 响应格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 |

## 2. 统一响应格式

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 错误响应

```json
{
  "code": "ERROR_CODE",
  "message": "错误描述",
  "details": {}
}
```

### 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "page_size": 20
  }
}
```

## 3. 接口列表

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 1 | GET | /api/v1/users | 获取用户列表 | 需要 |
| 2 | GET | /api/v1/users/:id | 获取用户详情 | 需要 |
| 3 | POST | /api/v1/users | 创建用户 | 需要 |
| 4 | PUT | /api/v1/users/:id | 更新用户 | 需要 |
| 5 | DELETE | /api/v1/users/:id | 删除用户 | 需要 |

## 4. 接口详细设计

### 4.1 获取用户列表

**请求**：

```
GET /api/v1/users?page=1&page_size=20&keyword=xxx
```

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| page_size | int | 否 | 每页条数，默认20 |
| keyword | string | 否 | 搜索关键词 |

**响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "username": "zhangsan",
        "email": "zhangsan@example.com",
        "created_at": "2026-01-01T00:00:00Z"
      }
    ],
    "total": 100,
    "page": 1,
    "page_size": 20
  }
}
```

### 4.2 {接口名称}

{按上述格式定义每个接口}

## 5. 错误码定义

| 错误码 | HTTP状态码 | 描述 |
|--------|-----------|------|
| USER_NOT_FOUND | 404 | 用户不存在 |
| USER_ALREADY_EXISTS | 409 | 用户已存在 |
| INVALID_PARAMETER | 400 | 参数校验失败 |
| UNAUTHORIZED | 401 | 未认证 |
| FORBIDDEN | 403 | 无权限 |
| INTERNAL_ERROR | 500 | 服务器内部错误 |
