# API 接口设计

> 项目：DocChat — 文档智能客服 SaaS
> 版本：V1
> 日期：2026-06-26
> 基线：基于 MVP API 设计增量扩展

## 1. 接口概述

| 维度 | 说明 |
|------|------|
| 基础路径 | /api/v1 |
| 认证方式 | Bearer Token (JWT) / API Key (`dc_` 前缀) |
| 响应格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 |

## 2. 统一响应格式

（继承 MVP，无变更）

## 3. 接口列表

### 3.1 MVP 已有接口（V1 继承，共 24 个）

（见 MVP API 设计文档，V1 变更标注如下）

| 接口 | V1 变更 |
|------|--------|
| POST /api/v1/chat/conversations | **变更**：支持 JWT + API Key 双鉴权 |
| GET /api/v1/widget/config | **变更**：支持 API Key 鉴权 |
| GET /api/v1/widget/embed-script | **变更**：嵌入代码使用 API Key |
| 其余 21 个接口 | 无变更 |

### 3.2 API Key 模块 (/api/v1/api-keys) — V1 新增

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 25 | POST | /api/v1/api-keys | 创建 API Key | JWT (ADMIN) |
| 26 | GET | /api/v1/api-keys | 获取 Key 列表 | JWT |
| 27 | DELETE | /api/v1/api-keys/{keyId} | 吊销 API Key | JWT (ADMIN) |
| 28 | PUT | /api/v1/api-keys/{keyId}/name | 修改 Key 名称 | JWT (ADMIN) |

### 3.3 统计模块 (/api/v1/stats) — V1 新增

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 29 | GET | /api/v1/stats/overview | 获取用量概览 | JWT |
| 30 | GET | /api/v1/stats/daily | 获取每日用量明细 | JWT |
| 31 | GET | /api/v1/stats/trend | 获取用量趋势（7/30天） | JWT |

### 3.4 评测模块 (/api/v1/eval) — V1 新增

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 32 | POST | /api/v1/eval/sets | 创建评测集 | JWT (ADMIN/MEMBER) |
| 33 | GET | /api/v1/eval/sets | 获取评测集列表 | JWT |
| 34 | GET | /api/v1/eval/sets/{setId} | 获取评测集详情 | JWT |
| 35 | PUT | /api/v1/eval/sets/{setId} | 更新评测集信息 | JWT (ADMIN/MEMBER) |
| 36 | DELETE | /api/v1/eval/sets/{setId} | 删除评测集 | JWT (ADMIN/MEMBER) |
| 37 | POST | /api/v1/eval/sets/{setId}/pairs | 添加问答对 | JWT (ADMIN/MEMBER) |
| 38 | GET | /api/v1/eval/sets/{setId}/pairs | 获取问答对列表 | JWT |
| 39 | DELETE | /api/v1/eval/sets/{setId}/pairs/{pairId} | 删除问答对 | JWT (ADMIN/MEMBER) |
| 40 | POST | /api/v1/eval/sets/{setId}/pairs/import | 批量导入问答对 | JWT (ADMIN/MEMBER) |
| 41 | POST | /api/v1/eval/sets/{setId}/run | 执行评测 | JWT (ADMIN/MEMBER) |
| 42 | GET | /api/v1/eval/sets/{setId}/results | 获取评测结果列表 | JWT |
| 43 | GET | /api/v1/eval/sets/{setId}/results/{resultId} | 获取评测结果详情 | JWT |

### 3.5 LLM 配置 (/api/v1/llm-config) — V1 新增

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 44 | GET | /api/v1/llm-config | 获取 LLM 配置 | JWT (ADMIN) |
| 45 | PUT | /api/v1/llm-config | 更新 LLM 配置 | JWT (ADMIN) |
| 46 | DELETE | /api/v1/llm-config | 删除 LLM 配置（恢复系统默认） | JWT (ADMIN) |
| 47 | POST | /api/v1/llm-config/test | 测试 LLM 连通性 | JWT (ADMIN) |

### 3.6 租户限额配置 (/api/v1/tenants/current) — V1 变更

| # | 方法 | 路径 | 描述 | 认证 |
|---|------|------|------|------|
| 48 | PATCH | /api/v1/tenants/current/daily-limit | 设置每日对话限额 | JWT (ADMIN) |

## 4. 接口详细设计

### 4.1 POST /api/v1/api-keys — 创建 API Key

**请求**：

```json
{
  "name": "我的网站客服"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| name | String | 否 | 1-50字符 | Key 名称（可选） |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "我的网站客服",
    "key": "dc_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "keyPrefix": "dc_a1b2",
    "status": 1,
    "createdAt": "2026-06-26T10:00:00Z"
  }
}
```

**重要**：`key` 字段仅在创建时返回完整值，后续接口不再返回。管理员必须在此刻保存。

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| APIKEY_LIMIT_EXCEEDED | 400 | 每个租户最多 5 个有效 Key |
| APIKEY_NAME_DUPLICATE | 409 | Key 名称已存在 |

### 4.2 GET /api/v1/api-keys — 获取 Key 列表

**请求**：

```
GET /api/v1/api-keys
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "我的网站客服",
        "keyPrefix": "dc_a1b2",
        "keyMasked": "dc_****o5p6",
        "status": 1,
        "lastUsedAt": "2026-06-26T09:30:00Z",
        "createdAt": "2026-06-26T10:00:00Z"
      }
    ],
    "total": 2
  }
}
```

**注意**：返回脱敏的 Key（`keyMasked`），不返回完整 Key。

### 4.3 DELETE /api/v1/api-keys/{keyId} — 吊销 API Key

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
  "data": {
    "id": 1,
    "status": 0,
    "revokedAt": "2026-06-26T11:00:00Z"
  }
}
```

**说明**：吊销后立即生效。使用该 Key 的后续请求返回 401。

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| APIKEY_NOT_FOUND | 404 | Key 不存在 |
| APIKEY_ALREADY_REVOKED | 400 | Key 已被吊销 |

### 4.4 GET /api/v1/stats/overview — 获取用量概览

**请求**：

```
GET /api/v1/stats/overview?period=7d
```

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| period | String | 否 | 7d | 统计周期：7d / 30d |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "totalCalls": 1250,
    "totalTokens": 50000,
    "totalConversations": 120,
    "avgDailyCalls": 178,
    "avgDailyTokens": 7142,
    "avgDailyConversations": 17,
    "todayCalls": 45,
    "todayLimit": 1000,
    "todayRemaining": 955,
    "period": "7d"
  }
}
```

**说明**：只统计 `auth_type=API_KEY` 的正式对话，不包括 JWT 预览对话。

### 4.5 GET /api/v1/stats/daily — 获取每日用量明细

**请求**：

```
GET /api/v1/stats/daily?startDate=2026-06-19&endDate=2026-06-26
```

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| startDate | String | 否 | 7天前 | 起始日期（YYYY-MM-DD） |
| endDate | String | 否 | 今天 | 结束日期（YYYY-MM-DD） |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "list": [
      {
        "date": "2026-06-26",
        "calls": 45,
        "tokens": 2000,
        "conversations": 4
      }
    ],
    "total": 7
  }
}
```

### 4.6 GET /api/v1/stats/trend — 获取用量趋势

**请求**：

```
GET /api/v1/stats/trend?period=30d&metric=calls
```

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| period | String | 否 | 7d | 统计周期：7d / 30d |
| metric | String | 否 | calls | 指标：calls / tokens / conversations |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "metric": "calls",
    "period": "30d",
    "points": [
      {"date": "2026-06-01", "value": 15},
      {"date": "2026-06-02", "value": 20}
    ]
  }
}
```

### 4.7 POST /api/v1/eval/sets — 创建评测集

**请求**：

```json
{
  "name": "基础功能评测",
  "description": "验证核心功能检索质量"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| name | String | 是 | 1-100字符 | 评测集名称 |
| description | String | 否 | 最大500字符 | 描述 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "基础功能评测",
    "description": "验证核心功能检索质量",
    "pairCount": 0,
    "createdAt": "2026-06-26T10:00:00Z"
  }
}
```

### 4.8 POST /api/v1/eval/sets/{setId}/pairs — 添加问答对

**请求**：

```json
{
  "question": "如何重置密码？",
  "expectedDocument": "用户手册.pdf"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| question | String | 是 | 1-500字符 | 问题 |
| expectedDocument | String | 是 | 1-255字符 | 期望命中的文档名 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "evalSetId": 1,
    "question": "如何重置密码？",
    "expectedDocument": "用户手册.pdf",
    "sortOrder": 1,
    "createdAt": "2026-06-26T10:00:00Z"
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| EVAL_PAIR_LIMIT_EXCEEDED | 400 | 每个评测集最多 50 个问答对 |
| EVAL_SET_NOT_FOUND | 404 | 评测集不存在 |
| EVAL_SET_LIMIT_EXCEEDED | 400 | 每个租户最多 10 个评测集 |

### 4.9 POST /api/v1/eval/sets/{setId}/pairs/import — 批量导入问答对

**请求**：

```json
{
  "pairs": [
    {"question": "如何注册？", "expectedDocument": "用户手册.pdf"},
    {"question": "支持哪些文件格式？", "expectedDocument": "产品FAQ.md"}
  ]
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| pairs | Array | 是 | 每项含 question + expectedDocument | 问答对数组 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "importedCount": 2,
    "totalCount": 2
  }
}
```

### 4.10 POST /api/v1/eval/sets/{setId}/run — 执行评测

**请求**：

```
POST /api/v1/eval/sets/1/run
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "resultId": 1,
    "status": "RUNNING"
  }
}
```

**说明**：评测异步执行，通过 GET /api/v1/eval/sets/{setId}/results/{resultId} 查看进度和结果。

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| EVAL_ALREADY_RUNNING | 409 | 评测已在执行中 |
| EVAL_SET_EMPTY | 400 | 评测集无问答对 |
| EVAL_SET_NOT_FOUND | 404 | 评测集不存在 |

### 4.11 GET /api/v1/eval/sets/{setId}/results/{resultId} — 获取评测结果详情

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "evalSetId": 1,
    "hitRate": 85.00,
    "totalPairs": 10,
    "hitCount": 8,
    "status": "COMPLETED",
    "durationMs": 5000,
    "detail": [
      {
        "pairId": 1,
        "question": "如何重置密码？",
        "expectedDocument": "用户手册.pdf",
        "hit": true,
        "retrievedDocuments": ["用户手册.pdf", "产品FAQ.md", "安装指南.txt"],
        "topK": 5
      },
      {
        "pairId": 2,
        "question": "支持哪些文件格式？",
        "expectedDocument": "产品FAQ.md",
        "hit": false,
        "retrievedDocuments": ["安装指南.txt", "技术文档.pdf"],
        "topK": 5
      }
    ],
    "createdAt": "2026-06-26T10:00:00Z"
  }
}
```

### 4.12 PUT /api/v1/llm-config — 更新 LLM 配置

**请求**：

```json
{
  "apiUrl": "https://api.openai.com/v1/chat/completions",
  "apiKey": "sk-proj-xxxx",
  "modelName": "gpt-4o-mini"
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| apiUrl | String | 是 | 合法 HTTPS URL | LLM API 端点 URL |
| apiKey | String | 是 | 1-200字符 | LLM API 密钥（加密存储） |
| modelName | String | 是 | 1-50字符 | 模型名称 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "apiUrl": "https://api.openai.com/v1/chat/completions",
    "apiKeyMasked": "sk-****xxxx",
    "modelName": "gpt-4o-mini",
    "status": 1,
    "updatedAt": "2026-06-26T10:00:00Z"
  }
}
```

### 4.13 POST /api/v1/llm-config/test — 测试 LLM 连通性

**请求**：

```json
{
  "apiUrl": "https://api.openai.com/v1/chat/completions",
  "apiKey": "sk-proj-xxxx",
  "modelName": "gpt-4o-mini"
}
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "connected": true,
    "modelName": "gpt-4o-mini",
    "responseTimeMs": 350
  }
}
```

**错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| LLM_CONFIG_TEST_FAILED | 503 | LLM 连通性测试失败 |
| LLM_CONFIG_URL_INVALID | 400 | URL 格式不合法 |

### 4.14 PATCH /api/v1/tenants/current/daily-limit — 设置每日对话限额

**请求**：

```json
{
  "dailyChatLimit": 500
}
```

| 参数 | 类型 | 必填 | 校验规则 | 描述 |
|------|------|------|---------|------|
| dailyChatLimit | Integer | 是 | 1-100000 | 每日对话调用限额 |

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "dailyChatLimit": 500
  }
}
```

### 4.15 POST /api/v1/chat/conversations — V1 变更（双鉴权）

**V1 变更说明**：

原 MVP 接口仅接受 widget_token 鉴权。V1 变更后同时支持：

| Header 格式 | 鉴权类型 | 适用场景 | 用量统计 |
|------------|---------|---------|---------|
| `Authorization: Bearer dc_xxxx` | API Key | 正式访客对话 | ✅ 计入 |
| `Authorization: Bearer eyJ...` | JWT Token | 预览对话 | ❌ 不计入 |

**V1 新增错误码**：

| 错误码 | HTTP | 描述 |
|--------|------|------|
| APIKEY_INVALID | 401 | API Key 无效或已吊销 |
| APIKEY_QUOTA_EXCEEDED | 429 | 每日调用次数超限 |

## 5. 错误码定义

### 5.1 V1 新增模块错误码

| 模块 | 错误码范围 | 示例 |
|------|-----------|------|
| APIKEY | 408xx | APIKEY_NOT_FOUND=40801, APIKEY_INVALID=40802, APIKEY_LIMIT_EXCEEDED=40803, APIKEY_QUOTA_EXCEEDED=40804 |
| STAT | 409xx | STAT_INVALID_PERIOD=40901 |
| EVAL | 410xx | EVAL_SET_NOT_FOUND=41001, EVAL_SET_LIMIT_EXCEEDED=41002, EVAL_PAIR_LIMIT_EXCEEDED=41003, EVAL_ALREADY_RUNNING=41004, EVAL_SET_EMPTY=41005 |
| LLM_CONFIG | 411xx | LLM_CONFIG_URL_INVALID=41101, LLM_CONFIG_TEST_FAILED=41102 |

### 5.2 MVP 已有错误码（继承）

（见 MVP API 设计文档）

## 6. 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-26 | V1 初始版本：新增 24 个 API 接口 + 变更 3 个 MVP 接口 + 新增 4 个模块错误码 |
