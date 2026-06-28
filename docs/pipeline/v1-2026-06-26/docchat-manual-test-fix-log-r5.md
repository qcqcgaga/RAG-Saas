# 人工测试缺陷定位修复记录 — R5

> 项目：DocChat — 文档智能客服 SaaS
> 版本：v1-2026-06-26
> 修复日期：2026-06-28
> 修复轮次：R5（第五轮修复，针对四轮反复出现的问题做彻底根治）

## 缺陷清单

### D1: US-V1-006/010 — 评测执行报系统繁忙→提示已在执行中→无评测记录（三轮未修复）

**现象**：执行评测第一次报"系统繁忙"，再点提示"已在执行中"，评测结果列表始终为空。此问题在 R2/R3/R4 三轮修复后仍未解决。

**根因分析（彻底分析）**：

前三轮修复逐步解决了表层问题（API路径对齐、@Async→虚拟线程、单个pair容错），但**根本问题**一直未被触及：

1. **虚拟线程中 JPA 操作无事务管理**：`Thread.startVirtualThread()` 启动的虚拟线程不继承 Spring 的事务上下文。`evalResultRepository.findById(resultId)` 和 `evalResultRepository.save(r)` 在虚拟线程中执行时没有 EntityManager 和事务绑定，JPA 操作可能抛异常导致状态无法更新
2. **retrievalService.retrieve() 直接抛异常未被外层容错**：虽然 R3 为单个 pair 添加了 try-catch，但 `executeEval` 方法内部 `evalPairRepository.findByEvalSetIdOrderBySortOrder(setId)` 也在虚拟线程中执行，如果此 JPA 操作失败，整个评测流程中断
3. **Redis 锁清理不可靠**：虽然 finally 块中有 `redisTemplate.delete()`，但如果 JPA 操作在虚拟线程中彻底失败，finally 中的 `redisTemplate.delete()` 可能执行成功，但锁的 TTL 是10分钟，如果锁被其他原因（如 Redis 连接瞬断）误设，无法及时清理
4. **前端错误处理吞掉异常**：`handleExecute` 中 catch 块为空 `{ /* handled */ }`，用户看不到任何错误提示

**修复方案（彻底根治）**：

1. **新增 `EvalSelfService` 自引用服务**：Spring AOP 要求跨 Bean 调用才能触发 `@Transactional`。将虚拟线程中需要的 JPA 操作抽取到 `EvalSelfService`，使用 `REQUIRES_NEW` 传播级别确保每次调用有独立事务
2. **`safeRetrieve()` 完全容错**：新增 `safeRetrieve(question, tenantId)` 方法，将 `retrievalService.retrieve()` 的所有异常捕获，返回空列表而非抛异常。Milvus 不可用/collection不存在 = 所有 pair 未命中，而非评测流程中断
3. **Redis 锁 TTL 缩短为5分钟 + 前端增加"清理锁"按钮**：用户可在 UI 上直接清理卡住的评测锁（`DELETE /api/v1/eval/sets/{setId}/lock`）
4. **前端错误处理改进**：`handleExecute` 中不再静默吞掉错误，区分"已在执行中"和"其他错误"
5. **虚拟线程顶层异常兜底**：使用 `evalSelfService.markAsFailed()` 在独立事务中更新状态，而非直接操作 `evalResultRepository`

**修改文件**：
- `server/.../module_eval/service/EvalSelfService.java` — 新增自引用服务
- `server/.../module_eval/service/EvalServiceImpl.java` — 重写 runEval + executeEval + 新增 safeRetrieve/clearLock
- `server/.../module_eval/service/EvalService.java` — 新增 clearLock 接口方法
- `server/.../module_eval/controller/EvalController.java` — 新增 DELETE /sets/{setId}/lock 端点
- `web/src/views/eval/index.vue` — 错误处理改进 + 清理锁按钮
- `web/src/api/eval.ts` — 无修改（路径已对齐）
- `server/.../test/StatServiceImplTest.java` — mock 类型修复

---

### D2: US-V1-007 — 预览窗口反复变空白（四轮出现）

**现象**：管理后台聊天组件配置页面的预览窗口反复变为空白。R2/R3/R4 各轮修复后问题仍重现。

**根因分析（彻底分析）**：

前四轮修复经历了 srcdoc→src URL→增加 data-token→增加调试日志的演进，但核心问题始终未被解决：

1. **`document.currentScript` 在 iframe 中不可靠**：`main.ts` 使用 `document.currentScript` 获取 `data-*` 属性，但在 iframe 中动态加载的 script，某些浏览器（特别是 Edge）中 `document.currentScript` 可能返回 `null`
2. **配置加载失败时组件静默退出**：`ChatWidget.init()` 中 `if (!this.config || !this.config.enabled) { return; }` — 如果 `fetchConfig` 返回 null（配置加载失败），组件创建实例但不渲染任何 DOM，导致"空白"
3. **缺少可靠的配置传递机制**：仅依赖 script 的 data-* 属性，当 `document.currentScript` 失效时，apiKey/token/apiUrl 全部为空字符串，`fetchConfig` 请求失败，组件静默退出

**修复方案（彻底根治）**：

1. **新增 `window.__DOCCHAT_CONFIG__` 全局变量传递机制**：`widget-preview.html` 在加载 widget.js 前设置全局变量 `window.__DOCCHAT_CONFIG__`，`main.ts` 优先从全局变量读取，再从 `document.currentScript.dataset` 读取作为兜底
2. **widget-preview.html 增强**：添加 script.onerror 错误处理，在 widget.js 加载失败时显示错误信息
3. **test-widget.html 重写**：移除硬编码的 widget token，改为引导用户手动粘贴嵌入脚本。添加错误提示框。明确说明必须通过 HTTP 服务器访问（不能使用 file:// 协议）

**修改文件**：
- `packages/chat-widget/src/main.ts` — 新增 window.__DOCCHAT_CONFIG__ 读取优先级
- `server/.../resources/static/widget-preview.html` — 重写，增加全局变量传递 + onerror 处理
- `packages/chat-widget/dist/` — 重新构建
- `server/.../resources/static/widget.js+css` — 更新
- `test-widget.html` — 重写

---

### D3: US-V1-004 — Token用量统计不增长

**现象**：用户使用聊天组件对话后，用量统计面板中 Token 消耗始终为0。

**根因分析**：

1. **`ChatStatAspect` 记录 promptTokens=0, completionTokens=0**：这是 R1-R4 中已知的 TODO。虽然调用量在增长，但 token 数始终为0
2. **LLM 响应中的 token 使用量未被提取**：Anthropic API 在 `message_start` 事件中返回 `input_tokens`，在 `message_delta` 事件中返回 `output_tokens`，但 `LlmService.parseSseResponse()` 只解析 `content_block_delta`，未提取 usage 信息
3. **AuthContext 缺少 token 使用量字段**：`AuthContext` 只有 tenantId/apiKeyId/authType/modelName，没有 promptTokens/completionTokens 字段来传递从 LLM 响应提取的 token 使用量

**修复方案**：

1. **`LlmService` 新增 `TokenUsage` 类**：`streamChat()` 返回 `TokenUsage`（包含 promptTokens/completionTokens），从 Anthropic SSE 响应的 `message_start.usage.input_tokens` 和 `message_delta.usage.output_tokens` 中提取
2. **`AuthContext` 新增 token 使用量字段**：新增 `PROMPT_TOKENS` 和 `COMPLETION_TOKENS` ThreadLocal，以及 `setTokenUsage()`/`getPromptTokens()`/`getCompletionTokens()` 方法
3. **`ChatServiceImpl` 在 LLM 调用完成后设置 token 使用量**：`executeConversation()` 中 `llmService.streamChat()` 返回 `TokenUsage`，调用 `AuthContext.setTokenUsage()` 写入
4. **`ChatStatAspect` 从 AuthContext 读取实际 token 数**：不再记录固定的 0，改为 `AuthContext.getPromptTokens()` 和 `AuthContext.getCompletionTokens()`

**修改文件**：
- `server/.../module_chat/service/LlmService.java` — 新增 TokenUsage + parseSseResponse 提取 usage
- `server/.../module_chat/aop/AuthContext.java` — 新增 token 使用量字段
- `server/.../module_chat/service/ChatServiceImpl.java` — 设置 AuthContext token 使用量
- `server/.../module_chat/aop/ChatStatAspect.java` — 读取 AuthContext 实际 token 数

---

### D4: US-V1-008 — 团队成员仍能看到 LLM 配置页签

**现象**：团队成员账号登录后，左侧导航栏仍显示"LLM 配置"菜单项。

**根因分析**：

1. **AppLayout 中 `v-if="userStore.isAdmin"` 可能读到过时值**：当用户从管理员切换到成员账号时，Pinia store 的响应式更新可能有延迟，导致菜单在 `onMounted` 执行前仍使用旧的 ADMIN role 渲染
2. **缺少 role 变化的 watch 监听**：`AppLayout` 只在 `onMounted` 中同步 localStorage role，但没有 watch 监听 role 变化

**修复方案**：

1. **双重检查 computed 属性**：`isAdminMenu` 同时检查 `userStore.role === 'ADMIN'` 和 `localStorage.getItem('role') === 'ADMIN'`，任一条件满足才显示
2. **watch 监听 userStore.role**：当 role 变化时自动同步到 localStorage
3. **菜单 v-if 改用 isAdminMenu**：取代直接使用 `userStore.isAdmin`

**修改文件**：
- `web/src/components/layout/AppLayout.vue` — isAdminMenu computed + watch role + v-if 改用 isAdminMenu

---

## 修改文件汇总

| 文件 | 修改类型 | 关联缺陷 |
|------|----------|----------|
| `server/.../module_eval/service/EvalSelfService.java` | 新增 | D1 |
| `server/.../module_eval/service/EvalServiceImpl.java` | 重写runEval+executeEval | D1 |
| `server/.../module_eval/service/EvalService.java` | 新增clearLock接口 | D1 |
| `server/.../module_eval/controller/EvalController.java` | 新增DELETE /lock端点 | D1 |
| `web/src/views/eval/index.vue` | 错误处理+清理锁按钮 | D1 |
| `packages/chat-widget/src/main.ts` | 全局变量优先读取 | D2 |
| `server/.../resources/static/widget-preview.html` | 重写 | D2 |
| `test-widget.html` | 重写 | D2 |
| `server/.../module_chat/service/LlmService.java` | TokenUsage+usage提取 | D3 |
| `server/.../module_chat/aop/AuthContext.java` | 新增token使用量字段 | D3 |
| `server/.../module_chat/service/ChatServiceImpl.java` | 设置AuthContext token | D3 |
| `server/.../module_chat/aop/ChatStatAspect.java` | 读取实际token数 | D3 |
| `web/src/components/layout/AppLayout.vue` | isAdminMenu+watch | D4 |
| `server/.../test/StatServiceImplTest.java` | mock类型修复 | D1 |
| `packages/chat-widget/dist/` | 重新构建 | D2 |
| `server/.../resources/static/widget.js+css` | 更新 | D2 |

## 编译验证

- ✅ 后端 `mvn compile` 通过
- ✅ 后端 146 个单元测试全部通过（含 StatServiceImplTest mock 类型修复）
- ✅ 前端 `vue-tsc --noEmit` 类型检查通过
- ✅ chat-widget `vite build` 通过
- ✅ widget.js/widget.css 已复制到 server static

## 经验教训

### L-MANUAL-011: 虚拟线程中 JPA 操作必须通过独立事务服务

```yaml
- id: L-MANUAL-011
  title: 虚拟线程JPA需独立事务
  description: |
    Thread.startVirtualThread() 不继承 Spring 事务上下文，JPA 操作在虚拟线程中可能因无 EntityManager 而失败。
    对策：将虚拟线程中需要的 JPA 操作抽取到独立 Service Bean，使用 @Transactional(propagation = REQUIRES_NEW)。
    这是 Spring AOP 代理限制的直接应用：同类内部调用不触发 @Transactional。
  date: 2026-06-28
  source: 项目实践-V1人工测试R5修复
```

### L-MANUAL-012: 外部服务调用必须完全容错

```yaml
- id: L-MANUAL-012
  title: 外部调用需完全容错
  description: |
    评测执行依赖 Milvus/Embedding 等外部服务，这些服务不可用时不应导致整个评测流程中断。
    对策：将 retrievalService.retrieve() 包装为 safeRetrieve()，异常时返回空列表而非抛异常。
    三轮未修复的评测问题，根因就是 Milvus 不可用时 retrievalService 直接抛异常。
    "检索失败 = 未命中" 比 "检索失败 = 评测崩溃" 合理得多。
  date: 2026-06-28
  source: 项目实践-V1人工测试R5修复
```

### L-MANUAL-013: iframe中document.currentScript不可靠，用全局变量兜底

```yaml
- id: L-MANUAL-013
  title: iframe中currentScript不可靠
  description: |
    document.currentScript 在 iframe 中动态加载的 script 上可能返回 null（某些浏览器特别是 Edge）。
    对策：在 iframe 中使用 window.__DOCCHAT_CONFIG__ 全局变量传递配置，优先级高于 dataset。
    预览窗口反复变空白四轮，根因就是 currentScript 失效导致 apiKey/token 全为空，ChatWidget 静默退出。
  date: 2026-06-28
  source: 项目实践-V1人工测试R5修复
```

### L-MANUAL-014: LLM token使用量必须从SSE响应中提取

```yaml
- id: L-MANUAL-014
  title: LLM token量需从SSE提取
  description: |
    Anthropic API 在 SSE 响应中通过 message_start.usage.input_tokens 和 message_delta.usage.output_tokens 返回 token 使用量。
    如果不提取这些信息，用量统计中 Token 消耗永远为 0。
    对策：LlmService.streamChat() 返回 TokenUsage，通过 AuthContext 传递给 ChatStatAspect。
  date: 2026-06-28
  source: 项目实践-V1人工测试R5修复
```

### L-MANUAL-015: 前端权限检查需双重校验（store + localStorage）

```yaml
- id: L-MANUAL-015
  title: 权限检查需双重校验
  description: |
    Pinia store 的响应式更新可能有延迟（特别是用户切换账号时），导致 v-if 条件读到过时值。
    对策：computed 属性同时检查 userStore.role 和 localStorage.getItem('role')，
    任一条件满足才显示管理员菜单。watch 监听 role 变化自动同步 localStorage。
  date: 2026-06-28
  source: 项目实践-V1人工测试R5修复
```

## 待用户验证

重启服务后需验证：US-V1-004、US-V1-006、US-V1-007、US-V1-008、US-V1-010
