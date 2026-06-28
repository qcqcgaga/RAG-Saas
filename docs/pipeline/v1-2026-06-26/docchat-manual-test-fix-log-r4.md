# 人工测试缺陷定位修复记录 — R4

> 项目：DocChat — 文档智能客服 SaaS
> 版本：v1-2026-06-26
> 修复日期：2026-06-28
> 修复轮次：R4（第四轮修复，修复第四轮人工测试发现的缺陷）

## 缺陷清单

### D1: US-V1-004 — 用量统计面板数据全部为0

**现象**：用户使用 粑粑店1 账号调用了三次对话，但用量统计面板中总调用量/总 Token 消耗/总对话数/每日统计中的 token 字段都为0。

**根因分析**：

1. **JPQL `CAST(l.createdAt AS date)` 在 PostgreSQL 上兼容性问题**：Hibernate + PostgreSQL 驱动对 JPQL 的 `CAST` 表达式处理不一致，可能抛运行时异常或返回意外类型，导致 `dailyAggregateByTenantAndRange` 查询失败，异常被 `StatServiceImpl` 的 catch 块吞掉返回默认值0
2. **原生查询返回类型不一致**：PostgreSQL 原生查询返回 `BigInteger`/`BigDecimal`，而代码中直接 `((Number) row[1]).longValue()` 可能抛 ClassCastException
3. **`convertToDate` 类型处理不全**：只处理了 `java.sql.Date` 和 `LocalDate`，PostgreSQL 原生查询可能返回 `java.sql.Timestamp` 或其他类型
4. **`ChatUsageLog.authType` 注释过时**：实体注释仍写着"API_KEY / JWT"，遗漏了 V1 新增的 WIDGET 类型
5. **Token 消耗为0是已知限制**：`ChatStatAspect` 中 `promptTokens=0, completionTokens=0` 是 TODO，待 LLM 接入后补充

**修复方案**：
1. `dailyAggregateByTenantAndRange` 改用 native query，使用 PostgreSQL 原生 `DATE()` 函数替代 JPQL `CAST`
2. 每日统计解析改用 `toLong()` 安全转换，兼容 BigInteger/BigDecimal/Long/Integer 等多种类型
3. `convertToDate` 增加 `java.sql.Timestamp`、`java.util.Date` 等类型处理，增加异常兜底
4. `ChatUsageLog.authType` 注释更新为"API_KEY / WIDGET / JWT"
5. `ChatStatAspect` 增加日志级别（debug→info），便于排查用量记录是否触发

**修改文件**：
- `server/.../module_stat/repository/ChatUsageLogRepository.java` — dailyAggregate 改 native query
- `server/.../module_stat/service/StatServiceImpl.java` — toLong 替代直接转型 + convertToDate 增强
- `server/.../module_stat/entity/ChatUsageLog.java` — authType 注释更新
- `server/.../module_chat/aop/ChatStatAspect.java` — 日志增强

---

### D2: US-V1-005/006 — 评测集接口报404"请求的资源不存在"

**现象**：点击评测集页签时，调用 `http://localhost/api/v1/eval-sets` 报错"请求的资源不存在"。运行评测报系统繁忙，再点提示已在执行中。

**根因分析**：

1. **R3 修复后服务未重启**：R3 已将前端 API 路径从 `/api/v1/eval-sets` 修正为 `/api/v1/eval/sets`，与后端 `EvalController` 的 `@RequestMapping("/api/v1/eval")` + `@PostMapping("/sets")` 对齐
2. **但运行中的服务仍是 R3 之前的旧代码**，前端代码未重新构建部署
3. 当前代码中前端 `web/src/api/eval.ts` 和后端 `EvalController.java` 路径已完全对齐，无需额外修改

**修复方案**：
1. 无代码修改，需要**重启服务**使 R3 修复生效
2. 重新构建 chat-widget 并复制到 server static 目录

**修改文件**：
- 无（路径已对齐，需重启服务）

---

### D3: US-V1-007 — 聊天组件预览显示但无法点击输入

**现象**：管理后台聊天组件配置页面的预览窗口中，组件渲染出来了但无法点击输入框进行对话。

**根因分析**：

1. **`widget-preview.html` 中 `data-token` 属性缺失**：R3 修复只设置了 `data-api-key`，没有设置 `data-token`。`main.ts` 中 `token = scriptEl?.dataset.token || apiKey`，当 `dataset.token` 为 undefined 时回退到 `apiKey`，这本身可以工作
2. **但 `data-api-url` 也通过属性设置**，而 `main.ts` 读取 `scriptEl?.dataset.apiUrl`。动态插入的 `<script>` 标签上 `dataset.apiUrl` 对应 `data-api-url` 属性，这应该能正确读取
3. **更可能的问题**：iframe 中加载的预览页面，`widget.js` 的 `fetchConfig` 调用可能因跨域或网络问题失败，导致组件虽然创建了 DOM 但配置为 null，输入框事件绑定异常
4. **调试信息不足**：`ChatWidget` 和 `api.ts` 中缺少 console.log，无法在浏览器 DevTools 中定位问题

**修复方案**：
1. `widget-preview.html` 同时设置 `data-api-key` 和 `data-token` 属性，确保 `main.ts` 能正确读取
2. `main.ts` 增加初始化参数调试日志
3. `ChatWidget.init()` 增加配置加载成功/失败日志
4. `ChatWidget.loadConfig()` 增加异常捕获和日志
5. `api.ts fetchConfig()` 增加 HTTP 状态码检查和请求 URL 日志

**修改文件**：
- `server/.../resources/static/widget-preview.html` — 增加 data-token 属性
- `packages/chat-widget/src/main.ts` — 增加调试日志
- `packages/chat-widget/src/ChatWidget.ts` — 增加初始化和配置加载日志
- `packages/chat-widget/src/api.ts` — 增加 HTTP 错误检查和请求日志
- `packages/chat-widget/dist/` — 重新构建
- `server/.../resources/static/widget.js+css` — 更新

---

### D4: US-V1-008 — 团队成员看到 LLM 配置页签

**现象**：团队成员账号登录后，左侧导航栏仍显示"LLM 配置"菜单项。

**根因分析**：

1. **`AppLayout.vue` 已有 `v-if="userStore.isAdmin"` 控制**，路由守卫也有 `requiresAdmin` 检查
2. **但 `userStore` 的 `role` 初始化来自 `localStorage`**：`const role = ref<string>(localStorage.getItem('role') || '')`
3. **时序问题**：当用户从管理员切换到成员登录时，如果 `AppLayout` 组件在 `setAuth` 之前渲染（或 `setAuth` 的响应式更新延迟），可能短暂显示管理员菜单
4. **更可能的原因**：用户在同一个浏览器中测试多个账号，localStorage 的 role 值在页面刷新前可能不一致

**修复方案**：
1. `AppLayout.vue` 的 `onMounted` 中增加 localStorage role 同步逻辑：如果 localStorage 中的 role 与 userStore.role 不一致，强制更新 userStore.role
2. 这确保每次 AppLayout 挂载时，菜单权限与最新登录状态一致

**修改文件**：
- `web/src/components/layout/AppLayout.vue` — onMounted 增加 role 同步

## 修改文件汇总

| 文件 | 修改类型 | 关联缺陷 |
|------|----------|----------|
| `server/.../module_stat/repository/ChatUsageLogRepository.java` | native query 替代 JPQL CAST | D1 |
| `server/.../module_stat/service/StatServiceImpl.java` | toLong 安全转换 + convertToDate 增强 | D1 |
| `server/.../module_stat/entity/ChatUsageLog.java` | 注释更新 | D1 |
| `server/.../module_chat/aop/ChatStatAspect.java` | 日志增强 | D1 |
| `server/.../resources/static/widget-preview.html` | 增加 data-token 属性 | D3 |
| `packages/chat-widget/src/main.ts` | 增加调试日志 | D3 |
| `packages/chat-widget/src/ChatWidget.ts` | 增加初始化和配置加载日志 | D3 |
| `packages/chat-widget/src/api.ts` | 增加 HTTP 错误检查和请求日志 | D3 |
| `packages/chat-widget/dist/` | 重新构建 | D3 |
| `server/.../resources/static/widget.js+css` | 更新 | D3 |
| `web/src/components/layout/AppLayout.vue` | onMounted role 同步 | D4 |

## 编译验证

- ✅ 后端 `mvn compile` 通过
- ✅ 后端 146 个单元测试全部通过
- ✅ 前端 `vue-tsc --noEmit` 类型检查通过
- ✅ chat-widget `vite build` 通过

## 经验教训沉淀

### L-MANUAL-008: JPQL CAST 在 PostgreSQL 上不可靠

```yaml
- id: L-MANUAL-008
  title: JPQL CAST在PG上不可靠
  description: |
    JPQL 的 CAST(expr AS type) 在 Hibernate + PostgreSQL 组合下行为不一致：
    1. CAST(timestamp AS date) 可能抛异常或返回非预期类型
    2. H2 内存库测试通过但 PostgreSQL 上失败（H2 宽容，PG 严格）
    3. 对策：涉及日期函数的聚合查询改用 native query，使用数据库原生函数
    4. native query 返回类型多样（BigInteger/BigDecimal/Long/Integer），必须用 toLong() 安全转换
  date: 2026-06-28
  source: 项目实践-V1人工测试R4修复
```

### L-MANUAL-009: iframe 中动态加载 script 需同时设置所有 data-* 属性

```yaml
- id: L-MANUAL-009
  title: iframe动态script需全属性
  description: |
    在 iframe 中通过 createElement('script') 动态加载 widget.js 时：
    1. 必须同时设置 data-api-key、data-token、data-api-url 三个属性
    2. 缺少任一属性可能导致 ChatWidget 初始化参数不完整，组件渲染但无法交互
    3. 增加 console.log 调试日志，便于在浏览器 DevTools 中定位 iframe 内部问题
  date: 2026-06-28
  source: 项目实践-V1人工测试R4修复
```

### L-MANUAL-010: localStorage role 需与 userStore 同步

```yaml
- id: L-MANUAL-010
  title: localStorage role需同步
  description: |
    Pinia store 的 role 从 localStorage 初始化，但组件挂载时可能读取到过时值：
    1. 用户切换账号后，localStorage 已更新但 userStore.role 可能延迟
    2. AppLayout.onMounted 中应主动同步 localStorage role → userStore.role
    3. 这确保菜单权限（v-if="userStore.isAdmin"）与最新登录状态一致
  date: 2026-06-28
  source: 项目实践-V1人工测试R4修复
```

## 待用户验证

重启服务后需验证：US-V1-004、US-V1-005、US-V1-006、US-V1-007、US-V1-008、US-V1-010
