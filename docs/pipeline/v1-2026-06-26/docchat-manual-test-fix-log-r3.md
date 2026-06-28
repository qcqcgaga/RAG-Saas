# 人工测试缺陷定位修复记录 — R3

> 项目：DocChat — 文档智能客服 SaaS
> 版本：v1-2026-06-26
> 修复日期：2026-06-28
> 修复轮次：R3（第三轮修复，修复第三轮人工测试发现的缺陷）

## 缺陷清单

### D1: US-V1-004 — 嵌入式聊天组件对话后用量统计不增加

**现象**：在内嵌聊天组件中进行了一轮对话，但用量统计面板中调用次数未增加。用户同时困惑 API Key 的作用。

**根因分析**：

1. **后端查询过滤问题**：`ChatUsageLogRepository` 中所有 JPQL 查询都硬编码了 `AND l.authType = 'API_KEY'`，将 `authType = 'WIDGET'` 的记录完全过滤掉
2. **Widget Token 鉴权的对话已正确写入 `chat_usage_logs`**（authType='WIDGET'），但统计面板查询不到
3. **API Key 的概念澄清**：
   - **Widget Token**（UUID 格式）：嵌入式聊天组件使用，通过 `data-api-key` 传入，用于获取组件配置 + 对话鉴权，**应计入用量**
   - **API Key**（`dc_` 前缀）：REST API 调用使用，用于程序化对话鉴权，**应计入用量 + 受每日限额限制**
   - **JWT Token**（`eyJ` 前缀）：管理后台预览对话使用，**不计入用量**

**修复方案**：
1. `ChatUsageLogRepository` 的 3 个查询方法：`authType = 'API_KEY'` → `authType <> 'JWT'`，包含 API_KEY 和 WIDGET 两种鉴权类型
2. `ChatStatAspect` 注释更新，明确 WIDGET 类型也会记录用量

**修改文件**：
- `server/.../module_stat/repository/ChatUsageLogRepository.java` — 3 个查询条件修改
- `server/.../module_chat/aop/ChatStatAspect.java` — 注释更新

---

### D2: US-V1-005 — 批量导入评测对显示"已导入 undefined 条"

**现象**：批量导入 JSON 格式评测对后，提示消息为"已导入 undefined 条"。

**根因分析**：
1. `ImportResult` 类字段名为 `importedCount` / `totalCount`
2. Jackson 序列化后 JSON 为 `{"importedCount": N, "totalCount": M}`
3. 前端 `eval/index.vue` 读取 `res.data.imported`，字段名不匹配，得到 `undefined`

**修复方案**：
1. `ImportResult` 字段重命名：`importedCount` → `imported`，`totalCount` → `total`
2. Jackson 序列化后 JSON 为 `{"imported": N, "total": M}`，与前端匹配

**修改文件**：
- `server/.../module_eval/service/ImportResult.java` — 字段重命名
- `server/.../module_eval/service/EvalServiceImplTest.java` — 测试断言更新

---

### D3: US-V1-006 — 评测报系统繁忙 + 再点提示已在执行中

**现象**：执行评测第一次报"系统繁忙"，再点提示"已在执行中"，但评测结果列表为空。R2 修复后问题仍然存在。

**根因分析**：

1. **R2 修复已将 `@Async` 改为 `Thread.startVirtualThread()`**，异步执行本身正确
2. **核心问题**：`executeEval()` 在虚拟线程中调用 `retrievalService.retrieve()`，如果 Milvus 不可用或检索异常，整个评测流程中断
3. **单个问答对检索异常导致整体评测失败**：原代码中 for 循环内 `retrievalService.retrieve()` 抛异常直接传播到外层 catch
4. **Redis 锁清理问题**：虽然 finally 块中有 `redisTemplate.delete(lockKey)`，但如果 JPA save 在虚拟线程中失败（事务问题），finally 可能不执行
5. **前端 hitRate 显示错误**：后端 `hitRate` 已是 0-100 的百分比值，但前端 `Math.round(record.hitRate * 100)` 会将其放大 100 倍

**修复方案**：
1. `executeEval()` 中 for 循环内每个问答对独立 try-catch，单个检索失败不影响整体评测
2. 虚拟线程顶层增加异常兜底，确保 Redis 锁一定被清理
3. 捕获 `resultId` 避免虚拟线程中 JPA 实体脱离 Session
4. 前端 `hitRate` 显示：`record.hitRate * 100` → `record.hitRate`

**修改文件**：
- `server/.../module_eval/service/EvalServiceImpl.java` — 评测执行健壮性提升
- `web/src/views/eval/index.vue` — hitRate 显示修复

---

### D4: US-V1-007 — 聊天组件预览空白

**现象**：管理后台聊天组件配置页面的预览窗口完全空白，无法操作。

**根因分析**：

1. R2 修复将预览改为 `srcdoc` iframe，但 **`srcdoc` iframe 的 origin 是 null**（about:srcdoc）
2. 从 null origin 加载外部资源（`http://localhost:3000/widget.js`、`/widget.css`）受同源策略限制
3. `sandbox="allow-scripts allow-same-origin"` 虽然允许脚本执行，但 `srcdoc` 的 origin 仍然是 null，无法正确加载跨域资源

**修复方案**：
1. 放弃 `srcdoc` 方案，改用 `src` URL 指向后端静态预览页面
2. 后端新增 `static/widget-preview.html` 预览页面，通过 URL 参数接收 token 和 apiUrl
3. 预览页面动态创建 `<script>` 标签加载 `widget.js`，传入 `data-api-key` 和 `data-api-url`
4. Vite 代理新增 `/widget-preview.html` 转发到后端
5. `WidgetView.vue` 将 `srcdoc` 改为 `src`，去掉 `sandbox`

**修改文件**：
- `server/.../resources/static/widget-preview.html` — 新增预览页面
- `web/vite.config.ts` — 新增代理规则
- `web/src/views/widget/WidgetView.vue` — iframe src 改造 + TS 类型修复
- `packages/chat-widget/dist/` — 重新构建
- `server/.../resources/static/widget.js+css` — 更新

## 修改文件汇总

| 文件 | 修改类型 | 关联缺陷 |
|------|----------|----------|
| `server/.../module_stat/repository/ChatUsageLogRepository.java` | 查询条件修改 | D1 |
| `server/.../module_chat/aop/ChatStatAspect.java` | 注释更新 | D1 |
| `server/.../module_eval/service/ImportResult.java` | 字段重命名 | D2 |
| `server/.../module_eval/service/EvalServiceImpl.java` | 健壮性提升 | D3 |
| `server/.../module_eval/service/EvalServiceImplTest.java` | 测试修复 | D2 |
| `web/src/views/eval/index.vue` | hitRate 显示修复 | D3 |
| `server/.../resources/static/widget-preview.html` | 新增 | D4 |
| `web/vite.config.ts` | 代理规则 | D4 |
| `web/src/views/widget/WidgetView.vue` | iframe 改造 | D4 |
| `packages/chat-widget/dist/` | 重新构建 | D4 |
| `server/.../resources/static/widget.js+css` | 更新 | D4 |

## 编译验证

- ✅ 后端 `mvn compile` 通过
- ✅ 后端 146 个单元测试全部通过
- ✅ 前端 `vue-tsc --noEmit` 类型检查通过
- ✅ chat-widget `vite build` 通过

## API Key 概念澄清（面向用户）

DocChat V1 有三种鉴权 Token，用途不同：

| Token 类型 | 前缀 | 用途 | 是否计入用量 | 是否受每日限额 |
|-----------|------|------|------------|--------------|
| API Key | `dc_` | REST API 调用鉴权（程序化接入） | ✅ 是 | ✅ 是 |
| Widget Token | UUID | 嵌入式聊天组件鉴权（网站访客） | ✅ 是 | ❌ 否（仅 API Key 有限额） |
| JWT Token | `eyJ` | 管理后台预览对话 | ❌ 否 | ❌ 否 |

**API Key 的核心作用**：为第三方系统集成提供鉴权凭证。开发者创建 API Key 后，在 REST API 请求的 `Authorization: Bearer dc_xxx` 中使用，系统会验证 Key 有效性、检查每日限额、记录用量。聊天组件使用 Widget Token 而非 API Key。

## 待用户验证

重启服务后需验证：US-V1-004、US-V1-005、US-V1-006、US-V1-007
