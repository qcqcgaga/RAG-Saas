# 聊天组件管理模块 (module-widget)

> 聊天组件配置、JS 脚本生成、外观设置、widget_token 管理

## 目录结构

```
module_widget/
├── controller/WidgetController.java
├── dto/
│   ├── WidgetConfigResponse.java
│   ├── UpdateWidgetConfigRequest.java
│   ├── EmbedScriptResponse.java
│   └── TokenResponse.java
├── entity/WidgetConfig.java
├── repository/WidgetConfigRepository.java
└── service/WidgetService.java / WidgetServiceImpl.java
```

## API — WidgetController

基础路径: `/api/v1/widget`（需 JWT，`/config?token=xxx` 公开）

| 方法 | 路径 | 说明 | 认证 | 响应 |
|------|------|------|------|------|
| GET | `/config?token=xxx` | 获取组件配置(公开) | widget_token | `R<WidgetConfigResponse>` |
| PUT | `/config` | 更新组件配置 | JWT | `R<WidgetConfigResponse>` |
| GET | `/embed-script` | 获取嵌入脚本 | JWT | `R<EmbedScriptResponse>` |
| POST | `/regenerate-token` | 重新生成 token | JWT | `R<TokenResponse>` |

## 数据模型 — widget_configs 表

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT, UNIQUE | 每租户一个配置 |
| brand_color | VARCHAR(7), DEFAULT '#1890ff' | 品牌色 |
| welcome_message | VARCHAR(200), DEFAULT '你好，有什么可以帮你的？' | 欢迎语 |
| icon_url | VARCHAR(500) | 自定义图标 |
| widget_token | VARCHAR(64), UNIQUE | 嵌入认证 token |
| enabled | SMALLINT, DEFAULT 1 | 是否启用 |
| created_at / updated_at | TIMESTAMPTZ | |

## 嵌入脚本

`GET /embed-script` 返回可嵌入任意网页的 JS 脚本：
```html
<script src="https://cdn.docchat.io/widget.js" data-token="{widget_token}"></script>
```

## 详细文档

- [data-model.md](data-model.md)
- [pitfalls.md](pitfalls.md)
