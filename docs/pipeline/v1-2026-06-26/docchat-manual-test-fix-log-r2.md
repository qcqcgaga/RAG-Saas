# 人工测试缺陷定位修复记录 — R2

> 项目：DocChat — 文档智能客服 SaaS
> 版本：v1-2026-06-26
> 修复日期：2026-06-28
> 修复轮次：R2（第二轮修复，修复人工测试正式验证发现的缺陷）

## 缺陷清单

### D1: US-V1-001 — 创建 API Key 后密钥文本框为空，复制得到 undefined

**现象**：创建 API Key 后，弹窗中密钥文本框为空，复制按钮复制的内容为 undefined。

**根因分析**：
- 后端 `ApiKeyResponse` 返回字段名为 `id` 和 `key`（完整 Key 在 `key` 字段中）
- 前端 `apikey.ts` 定义类型为 `{ keyId, keyPrefix, fullKey }`，与后端不匹配
- 前端 `index.vue` 中 `handleCreate` 使用 `res.data.fullKey`，但后端没有 `fullKey` 字段
- 前端表格使用 `row-key="keyId"` 和 `record.keyId`，但后端返回 `id`

**修复方案**：
1. 重写 `web/src/api/apikey.ts`：类型定义改为与后端 `ApiKeyResponse` 一致的 `ApiKeyData` 接口
2. 重写 `web/src/views/apikey/index.vue`：使用 `res.data.key` 获取完整 Key，`row-key="id"`，`record.id`

**修改文件**：
- `web/src/api/apikey.ts`（重写）
- `web/src/views/apikey/index.vue`（重写）

---

### D2: US-V1-002 — 吊销 Key 时 URL 含 undefined，报系统繁忙

**现象**：吊销 API Key 时，请求 URL 为 `/api/v1/api-keys/undefined`。

**根因分析**：D1 的根因同时导致 D2——`record.keyId` 为 undefined。D1 修复后自动解决。

---

### D3: US-V1-003 — API Key 管理界面没有显示每日限额

**现象**：API Key 管理页面没有显示每日限额信息。

**根因分析**：前端表格缺少每日限额列；后端 `ApiKeyResponse` 缺少 `dailyLimit` 字段。

**修复方案**：
1. 后端 `ApiKeyResponse` 新增 `dailyLimit` 字段
2. 后端 `toResponse()` 填充 `getDefaultQuotaLimit()`（1000）
3. 前端表格新增"每日限额"列

**修改文件**：
- `server/.../dto/ApiKeyResponse.java`（新增字段）
- `server/.../service/ApiKeyServiceImpl.java`（toResponse 填充）
- `web/src/views/apikey/index.vue`（新增列）

---

### D4+D5: US-V1-004 — 嵌入代码格式错误 + 聊天组件报"服务不可用"

**现象**：
1. 系统生成的嵌入代码不会显示聊天组件（URL硬编码 cdn.docchat.com，缺少 data-api-url）
2. 用户手动修改嵌入代码后组件能显示，但对话报"服务暂时不可用"

**根因分析**：
1. `WidgetServiceImpl.getEmbedScript()` URL 硬编码为 `https://cdn.docchat.com/widget.js`，缺少 `data-api-url`
2. chat-widget `main.ts` 使用 `document.getElementsByTagName('script')` 获取参数（不可靠）
3. **核心问题**：聊天组件使用 widget_token（UUID格式）作为鉴权凭证，但后端 `ChatController.resolveAuth` 只支持 `dc_` 和 `eyJ` 前缀，UUID 格式被判为无效

**修复方案**：
1. 嵌入脚本使用 `docchat.widget.base-url` 配置生成正确 URL，添加 `data-api-url`
2. chat-widget `main.ts` 改用 `document.currentScript`
3. `ChatController.resolveAuth` 新增 Widget Token 鉴权——UUID 格式 token 通过 `JdbcTemplate` 查询 `widget_configs` 获取 tenantId
4. `ChatWidget.ts` 区分 `apiKey`（对话鉴权）和 `widgetToken`（获取配置）

**修改文件**：
- `server/.../service/WidgetServiceImpl.java`（嵌入脚本 + baseUrl）
- `server/.../controller/ChatController.java`（Widget Token 鉴权 + JdbcTemplate）
- `server/.../resources/application.yml`（widget.base-url 配置）
- `packages/chat-widget/src/main.ts`（document.currentScript）
- `packages/chat-widget/src/ChatWidget.ts`（apiKey/widgetToken 区分）
- `packages/chat-widget/dist/`（重新构建）
- `server/.../resources/static/widget.js+css`（更新）

---

### D6: US-V1-004 — Widget 配置保存 PUT /api/v1/widget/config 返回 500

**现象**：保存配置时返回 500 Internal Server Error。

**根因分析**：
1. `UpdateWidgetConfigRequest.welcomeMessage` 有 `@Size(min=1)` 验证，前端初始值为空字符串触发验证失败
2. `UpdateWidgetConfigRequest.enabled` 是 `Short`，前端发送 `boolean`，类型不匹配
3. `WidgetConfigResponse.enabled` 是 `Short`，前端 `a-switch` 期望 `boolean`

**修复方案**：
1. `welcomeMessage` 改为 `@Size(max=200)`（去掉 min 限制）
2. `UpdateWidgetConfigRequest.enabled` 改为 `Boolean`
3. `WidgetConfigResponse.enabled` 改为 `Boolean`，转换 `Short→Boolean`
4. 前端 `welcomeMessage` 初始值改为非空默认值

**修改文件**：
- `server/.../dto/UpdateWidgetConfigRequest.java`（重写）
- `server/.../dto/WidgetConfigResponse.java`（enabled Boolean）
- `server/.../service/WidgetServiceImpl.java`（enabled 转换）
- `web/src/views/widget/WidgetView.vue`（初始值）

---

### D7: US-V1-005 — 批量导入评测对 JSON 报格式不正确

**现象**：用户使用 JSON 数组批量导入时前端报"JSON 格式不正确"。

**根因分析**：
1. 用户 JSON 中相邻元素间缺少逗号（JSON 语法错误）
2. 前端 catch 块对非 SyntaxError 错误静默吞掉
3. 缺少导入前的前置校验

**修复方案**：
1. 改进错误提示：SyntaxError 提示更详细，非 SyntaxError 显示具体信息
2. 新增前置校验：逐项检查 question/expectedDocument 非空

**修改文件**：
- `web/src/views/eval/index.vue`

---

### D8: US-V1-006 — 评测报系统繁忙，再点提示已在执行中，但无评测记录

**现象**：执行评测第一次报"系统繁忙"，再点提示"已在执行中"，评测结果列表为空。

**根因分析**：
1. `@Async` 在同类内部调用不生效（Spring AOP 代理限制），`executeEvalAsync` 实际同步执行
2. 检索异常直接传播到 `runEval()`，被 `GlobalExceptionHandler` 返回"系统繁忙"
3. Redis 锁已设置但异常后未清理，导致第二次请求检测到锁返回"已在执行中"

**修复方案**：
1. 改用 `Thread.startVirtualThread()` 替代 `@Async`，确保异步执行
2. 移除 `@Async` 注解和 import
3. 异步执行中异常不传播到 `runEval()`，Redis 锁在 finally 正确清理

**修改文件**：
- `server/.../service/EvalServiceImpl.java`

---

### D9: US-V1-007 — 预览窗口无法发送对话

**现象**：预览区域是纯 CSS 模拟的静态卡片，无法发送真实对话。

**根因分析**：`WidgetView.vue` 预览区域是手写静态 HTML/CSS，不是真实 chat-widget 实例。

**修复方案**：
1. 预览区域改为 iframe，使用 `srcdoc` 动态生成包含真实 chat-widget 的 HTML
2. 配置保存后通过 `postMessage` 发送 `config-update` 消息实现热更新
3. 新增"重置对话"按钮
4. 后端新增 `static/` 目录存放 widget.js/css
5. 前端 `vite.config.ts` 新增 widget.js/css 代理

**修改文件**：
- `web/src/views/widget/WidgetView.vue`（重写）
- `web/vite.config.ts`（代理规则）
- `server/.../resources/static/widget.js+css`（新增）

---

## 修改文件汇总

| 文件 | 修改类型 | 关联缺陷 |
|------|----------|----------|
| `web/src/api/apikey.ts` | 重写 | D1, D2 |
| `web/src/views/apikey/index.vue` | 重写 | D1, D2, D3 |
| `server/.../dto/ApiKeyResponse.java` | 新增字段 | D3 |
| `server/.../service/ApiKeyServiceImpl.java` | toResponse 填充 | D3 |
| `server/.../service/WidgetServiceImpl.java` | 嵌入脚本+enabled+baseUrl | D4, D5, D6 |
| `server/.../dto/UpdateWidgetConfigRequest.java` | 重写 | D6 |
| `server/.../dto/WidgetConfigResponse.java` | enabled Boolean | D6 |
| `server/.../controller/ChatController.java` | Widget Token鉴权 | D4, D5 |
| `server/.../resources/application.yml` | widget.base-url | D4, D5 |
| `packages/chat-widget/src/main.ts` | document.currentScript | D4, D5 |
| `packages/chat-widget/src/ChatWidget.ts` | apiKey/widgetToken | D4, D5 |
| `packages/chat-widget/dist/` | 重新构建 | D4, D5, D9 |
| `server/.../resources/static/widget.js+css` | 新增 | D9 |
| `web/src/views/widget/WidgetView.vue` | 重写iframe预览 | D6, D9 |
| `web/src/views/eval/index.vue` | 错误处理 | D7 |
| `server/.../service/EvalServiceImpl.java` | 虚拟线程 | D8 |
| `web/vite.config.ts` | widget代理 | D9 |

## 编译验证

- ✅ 后端 `mvn compile` 通过
- ✅ chat-widget `vite build` 通过

## 待用户验证

重启服务后需逐一验证：US-V1-001 至 US-V1-007
