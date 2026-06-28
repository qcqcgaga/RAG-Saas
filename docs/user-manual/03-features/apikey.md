# API Key 管理

> V1 新增功能

## 功能概述

API Key 管理模块为聊天组件提供专用的鉴权方式。通过 API Key，您可以控制聊天组件的访问权限、追踪每个 Key 的调用量，并设置每日调用限额防止滥用。

V1 版本中，API Key 替代旧版 `widget_token` 成为聊天组件的推荐鉴权方式。旧版 `data-token` 参数在过渡期仍然兼容。

## API Key 说明

### 什么是 API Key

API Key 是一串以 `dc_` 开头的字符串，用于标识和鉴权聊天组件的对话请求。每个 API Key 绑定到特定租户，通过 `Authorization: Bearer dc_xxxx` 请求头传递。

### API Key 与旧版 widget_token 的区别

| 对比项 | API Key（V1） | widget_token（MVP） |
|--------|---------------|---------------------|
| 格式 | `dc_` 开头的长字符串 | UUID 格式 |
| 数量 | 每租户最多 5 个 | 每租户 1 个 |
| 管理 | 可命名、吊销、重命名 | 仅可重新生成 |
| 统计 | 计入用量统计 | 不计入用量统计 |
| 限额 | 支持每日调用限额 | 无限额 |
| 嵌入参数 | `data-api-key` | `data-token` |

### 与鉴权模式的关系

V1 版本对话接口支持双鉴权模式：

| Token 前缀 | 鉴权方式 | 场景 | 统计 |
|------------|----------|------|------|
| `dc_` | API Key | 正式访客对话 | 计入用量统计 |
| `eyJ` | JWT | 管理后台预览对话 | 不计入用量统计 |

## 创建 API Key

### 操作步骤

1. 登录管理后台，进入 **API Key 管理** 页面

[截图：API Key 管理页面 - 空状态]

2. 点击 **创建 API Key** 按钮
3. 输入 Key 名称（如"生产环境"、"测试环境"），便于后续识别
4. 点击 **确认创建**

[截图：创建 API Key 弹窗]

5. 创建成功后，系统显示完整的 API Key 值

[截图：API Key 创建成功 - 显示完整 Key]

> **重要**：API Key 仅在创建时显示一次完整值，之后只能看到脱敏后的部分（如 `dc_ab****xyz`）。请务必立即复制并妥善保存。

### 限制说明

| 限制项 | 说明 |
|--------|------|
| 数量上限 | 每个租户最多 **5 个** API Key |
| 命名 | 名称不可重复，用于识别用途 |
| 安全 | Key 以 SHA-256 哈希存储，系统不保存明文 |

## 管理 API Key

### 查看 Key 列表

1. 进入 **API Key 管理** 页面
2. 列表显示以下信息：
   - **名称**：Key 的标识名称
   - **Key 值**：脱敏显示（如 `dc_ab****xyz`）
   - **状态**：有效 / 已吊销
   - **创建时间**：Key 的创建时间

[截图：API Key 列表]

### 重命名 API Key

1. 在 Key 列表中找到目标 Key
2. 点击 **重命名** 按钮
3. 输入新名称
4. 点击 **确认**

### 吊销 API Key

1. 在 Key 列表中找到目标 Key
2. 点击 **吊销** 按钮
3. 确认吊销操作

> **重要**：吊销操作不可撤销。吊销后，使用该 Key 的所有聊天组件将立即无法正常对话。请确保已替换为新的 Key 后再吊销旧 Key。

[截图：API Key 吊销确认弹窗]

## 在聊天组件中使用 API Key

### 嵌入脚本配置

V1 版本的嵌入脚本使用 `data-api-key` 参数：

```html
<script
  src="https://your-domain.com/widget.js"
  data-api-key="dc_your_api_key_here"
  data-api-url="https://your-domain.com"
></script>
```

### 旧版兼容

过渡期内，旧版 `data-token` 参数仍然可用：

```html
<!-- 旧版方式（过渡期兼容） -->
<script
  src="https://your-domain.com/widget.js"
  data-token="your-widget-token"
  data-api-url="https://your-domain.com"
></script>
```

> **建议**：新项目请使用 `data-api-key`，旧项目请尽快迁移。

### API 直接调用

如果需要直接调用对话 API，在请求头中传递 API Key：

```bash
curl -X POST https://your-domain.com/api/v1/chat/conversations \
  -H "Authorization: Bearer dc_your_api_key_here" \
  -H "Content-Type: application/json" \
  -d '{"question": "如何使用这个功能？"}'
```

## 每日调用限额

### 限额机制

V1 版本为每个租户设置每日对话调用次数硬限制：

- 每次通过 API Key 鉴权的对话请求会递增当日计数器
- 计数器每日零点重置
- 达到限额后，对话接口返回 `40804 - 每日调用次数超限` 错误

### 限额查看

您可以在 **用量统计** 页面查看当前周期的调用量，了解距离限额的余量。

详见 [用量统计](stats.md)。

## 安全建议

| 建议 | 说明 |
|------|------|
| 不要硬编码 | 避免将 API Key 写入源代码，使用环境变量或配置中心 |
| 定期轮换 | 建议定期创建新 Key 并吊销旧 Key |
| 按用途分离 | 为不同环境（生产/测试）创建不同的 Key |
| 立即吊销泄露 | 如果 Key 泄露，立即吊销并创建新 Key |
| 使用 HTTPS | 确保 API Key 在传输过程中加密 |
