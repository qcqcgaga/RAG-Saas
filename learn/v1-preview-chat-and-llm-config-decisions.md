# V1 预览对话与 LLM 配置 — 架构决策记录

> 记录 2026-06-26 `/product` + `/architect` 交互讨论中的关键决策。
> 约束文件已同步更新至 `.ai/product.md`、`.ai/structure.md`、`.ai/codeRule.md`。

---

## 1. 产品决策：聊天组件预览与模拟测试（特性 #9）

### 1.1 功能定义

管理后台组件配置页内嵌预览窗口，支持三项能力：

| 能力 | 说明 |
|------|------|
| 外观实时预览 | 修改品牌色/欢迎语/图标后，预览窗口立刻反映变化 |
| 模拟终端问答 | 在预览窗口直接输入问题，获得真实 RAG 回答 |
| 刷新重置 | 一键清空对话，恢复初始状态 |

### 1.2 关键产品决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 对话持久化 | 不持久化，纯前端临时展示 | 符合"对话即焚"原则，避免测试数据污染 |
| 用量统计 | 不计入 | 预览是管理员行为，不应消耗正式配额 |
| 特性范围 | 模拟问答 + 刷新重置 + 外观实时预览 | 用户要求包含外观实时预览 |
| 优先级 | P1（V1 阶段交付） | 显著提升管理员体验和质检效率 |

### 1.3 用户故事

- 作为**租户管理员**，我想要在配置组件外观后立刻看到效果，以便快速调整到满意的外观
- 作为**租户管理员/团队成员**，我想要在预览窗口直接提问测试问答效果，以便验证知识库检索质量
- 作为**租户管理员/团队成员**，我想要一键刷新预览窗口，以便清除测试对话重新开始

---

## 2. 架构决策：同路径原则（核心设计原则）

### 2.1 原则定义

> **同路径原则**：预览对话与正式访客对话走**完全相同的后端代码路径**，确保"预览 OK 则终端一定 OK"。

用户明确提出：选择可以确保在预览可用的情况下，终端用户也必然可用的方案，必须完全走同一条路。

### 2.2 原则影响

此原则直接否决了以下方案：

| 被否决方案 | 否决理由 |
|-----------|---------|
| 独立预览端点 `POST /api/v1/chat/preview` | 两条代码路径，无法保证一致性 |
| ChatRequest 扩展 `preview` 字段 | 预览和正式的请求结构不同，本质上仍是两条路径 |
| 增强静态 CSS 预览 | 渲染逻辑与真实 ChatWidget 不一致，预览 OK 不代表终端 OK |

### 2.3 最终方案

**对话路径**：同端点 + 鉴权方式区分

```
预览调用：JWT 鉴权 → POST /api/v1/chat/conversations → ChatService.converse() → 统计切面跳过
正式调用：widget_token 鉴权 → POST /api/v1/chat/conversations → ChatService.converse() → 统计切面记录
```

**预览窗口**：iframe 嵌入真实 ChatWidget + postMessage 配置同步

```
管理后台 WidgetView.vue
    ↓ 修改品牌色/欢迎语
    ↓ postMessage({ type: 'config-update', data: { brandColor, welcomeMessage } })
    ↓
iframe (加载真实 chat-widget JS)
    ↓ 接收 postMessage → 实时更新外观
    ↓ 用户在 iframe 内对话 → 走真实 /api/v1/chat/conversations 端点
    ↓
刷新按钮 → iframe.contentWindow.postMessage({ type: 'reset' }) → 清空对话
```

---

## 3. 架构决策：对话端点鉴权设计

### 3.1 方案对比

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A. 请求头标记** | 同端点，加 `X-Preview: true` Header | 改动最小 | 统计逻辑侵入 ChatService |
| **B. 独立预览端点** | 新增 `POST /api/v1/chat/preview`，JWT 鉴权 | 鉴权模型清晰 | ❌ 两条代码路径，违反同路径原则 |
| **C. ChatRequest 扩展字段** | 加 `preview: boolean` | 最简单 | ❌ 不安全，且两条路径 |
| **A+（最终选择）** | 同端点 + 鉴权方式自然区分 | 同路径 + 统计隔离 | Controller 需双鉴权解析 |

### 3.2 最终方案 A+ 详解

**核心设计**：
- 唯一对话端点：`POST /api/v1/chat/conversations`
- 鉴权方式不同：JWT（管理员预览）vs widget_token（访客正式）
- `ChatService.converse()` 内部**零感知**预览/正式区别
- 统计隔离在 **AOP 切面** `ChatStatAspect` 中，根据当前鉴权类型决定是否记录

**ChatController 调整**：
```java
@PostMapping("/conversations")
public SseEmitter converse(
    @RequestHeader("Authorization") String auth,
    @Valid @RequestBody ChatRequest request) {
    // 解析 auth：JWT → tenantId (预览), widget_token → tenantId (正式)
    Long tenantId = resolveTenantId(auth);
    return chatService.converse(request, tenantId);
    // 完全相同的调用路径！
}
```

**SecurityConfig 调整**：
- `/api/v1/chat/conversations` 保持 permitAll（两种鉴权方式都需要放行）
- 鉴权逻辑在 Controller 内部或 Filter 中区分

---

## 4. 架构决策：预览窗口实现

### 4.1 方案对比

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A. 增强静态预览** | 在 CSS 模拟预览上加对话能力 | 改动最小 | ❌ 渲染逻辑与真实 widget 不一致 |
| **B. iframe 嵌入真实 ChatWidget** | iframe + postMessage | 100% 一致 | 通信需 postMessage 机制 |
| **C. Vue 组件化 ChatWidget** | 适配为 Vue 组件 | 一致性好 | ❌ 需重构 ChatWidget，且适配层是"第二条路径" |
| **B+（最终选择）** | iframe + postMessage + ChatWidget 新增能力 | 100% 一致 + 刷新简单 | ChatWidget 需新增 postMessage 监听和 reset() |

### 4.2 ChatWidget 新增能力

| 能力 | 实现方式 | 说明 |
|------|---------|------|
| 外观热更新 | 监听 `postMessage({ type: 'config-update', data: {...} })` | 接收品牌色/欢迎语变更，实时更新 DOM |
| 刷新重置 | 监听 `postMessage({ type: 'reset' })` | 清空消息列表，恢复欢迎语 |
| postMessage 安全校验 | 校验 `event.origin` | 防止恶意页面注入配置 |

### 4.3 为什么不用 Vue 组件化

虽然方案 C 看似更"优雅"，但它违背了同路径原则：
- ChatWidget 是 IIFE 格式（原生 DOM 操作），强行适配 Vue 组件需要额外的适配层
- 适配层本身就是"第二条路径"——真实嵌入脚本走 IIFE，预览走 Vue 组件
- iframe 方式直接运行与嵌入脚本**完全相同的 JS 文件**，零适配

---

## 5. 决策：LLM API 真实配置

### 5.1 决策

| 维度 | 决策 |
|------|------|
| LLM 提供商 | 继续使用讯飞 Coding Plan API |
| 配置方式 | `application-prod.yml` 或环境变量注入 `docchat.llm.api-url` / `docchat.llm.api-key` |
| 代码影响 | 仅替换 `LlmService` 内部实现（mock → 真实 HTTP 调用），接口签名不变 |

### 5.2 现有基础设施

`LlmService` 已预留配置项：
```java
@Value("${docchat.llm.api-url:}")
private String apiUrl;

@Value("${docchat.llm.api-key:}")
private String apiKey;
```

### 5.3 V1 部署要求

V1 部署时必须配置真实 LLM API 地址和密钥，否则 RAG 对话服务不可用。此要求已写入 `product.md` V1 里程碑。

---

## 6. 约束文件变更汇总

| 文件 | 变更内容 |
|------|---------|
| `.ai/product.md` | ① 新增特性 #9「聊天组件预览与模拟测试」；② V1 里程碑补充 LLM API 配置要求 |
| `.ai/structure.md` | ① module-chat 新增 `aop/` 子目录（ChatStatAspect）；② 新增"V1 预览对话架构规则"（6 条规则：同路径原则、鉴权区分、统计隔离、ChatService 零感知、iframe 预览、ChatWidget 新增能力）；③ 模块描述更新；④ chat-widget 描述更新 |
| `.ai/codeRule.md` | 新增"预览对话安全"规则（5 条：同端点鉴权区分、禁止 widget_token 获得预览权限、统计隔离、ChatService 零感知、postMessage 安全校验） |
| `CLAUDE.md` | 当前状态更新：MVP ✅ 已完成，V1 目标已更新 |

---

## 7. 关键代码变更预览

```
后端：
├── ChatController.java        → 支持 JWT + widget_token 双鉴权解析
├── module-chat/aop/           → 新增 ChatStatAspect（AOP 统计切面）
├── SecurityConfig.java        → 调整 /api/v1/chat/conversations 的鉴权策略
├── LlmService.java            → 替换 mock 为真实讯飞 API 调用
└── application.yml            → docchat.llm.api-url / api-key 配置项（已预留）

前端：
├── WidgetView.vue             → 预览区改为 iframe，配置变更 postMessage 同步
└── api/chat.ts                → 新增预览对话 API 调用（复用同一端点，JWT 鉴权）

ChatWidget：
├── ChatWidget.ts              → 新增 postMessage 监听器 + reset() 方法
└── types.ts                   → 新增 PostMessage 事件类型定义
```
