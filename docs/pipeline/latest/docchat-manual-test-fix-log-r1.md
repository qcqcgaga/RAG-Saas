# 人工测试缺陷定位修复记录 — 第一轮

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0 (mvp-2026-06-24)
> 测试日期：2026-06-25
> 修复日期：2026-06-25

## 缺陷清单

| 缺陷ID | 关联用户故事 | 描述 | 严重度 | 状态 | 修复轮次 |
|---------|-------------|------|--------|------|---------|
| MT-DEF-01 | US-001 | 注册后 GET /api/v1/knowledge 返回 500 "系统繁忙" | P0 | ✅ 已修复 | 第1轮 |
| MT-DEF-02 | US-003 | 被邀请成员无法登录（随机密码未返回） | P0 | ✅ 已修复 | 第1轮 |
| MT-DEF-03 | US-004 | 上传 3.12MB 文件返回 413 Request Entity Too Large | P0 | ✅ 已修复 | 第1轮 |
| MT-DEF-04 | US-007 | GET /api/v1/widget/embed-script 返回 400 "聊天组件配置不存在" | P0 | ✅ 已修复 | 第1轮 |

---

## MT-DEF-01: 注册后 GET /api/v1/knowledge 返回 500

### 现象

新注册用户注册成功后，前端自动跳转到知识库页面，页面加载时请求 `GET /api/v1/knowledge` 返回：

```json
{"code": 50000, "msg": "系统繁忙，请稍后重试"}
```

### 根因分析

**直接原因**：`KnowledgeServiceImpl.getOrCreateKnowledgeBase()` 中 `SecurityUtil.getCurrentTenantId()` 返回 null，导致 `findByTenantId(null)` 返回空，`createDefaultKnowledgeBase(null)` 尝试用 null tenantId 插入 KnowledgeBase，违反数据库 NOT NULL 约束，抛出非 BizException 的 DataIntegrityViolationException，被 GlobalExceptionHandler 转为"系统繁忙"。

**根本原因**：`JwtAuthenticationFilter` 存在**双重注册**问题：
- `@Component` 注解使其被 Spring Boot 自动注册为 Servlet Filter
- `SecurityConfig.addFilterBefore()` 又在 SecurityFilterChain 中注册
- 作为 Servlet Filter 执行时，JwtAuthenticationFilter 设置了 SecurityContext 但 TenantFilter 尚未执行
- 后续 SecurityFilterChain 中 JwtAuthenticationFilter 的 `OncePerRequestFilter` 保护阻止其再次执行
- 结果：控制器运行时 TenantContext 为 null

### 修复方案

1. **治本**：在 `SecurityConfig` 中添加 `FilterRegistrationBean`，禁用 `JwtAuthenticationFilter` 的 Servlet 容器自动注册

2. **防御兜底**：在 `KnowledgeServiceImpl`、`WidgetServiceImpl` 等 Service 中，`getCurrentTenantId()` 后添加 null 检查，抛出清晰的 BizException

### 修改文件

| 文件 | 变更 |
|------|------|
| `common/config/SecurityConfig.java` | 新增 `jwtFilterRegistration()` Bean，`registration.setEnabled(false)` |
| `module_knowledge/service/KnowledgeServiceImpl.java` | `getOrCreateKnowledgeBase()` 添加 tenantId null 检查 |
| `module_widget/service/WidgetServiceImpl.java` | `updateConfig()`、`getEmbedScript()`、`regenerateToken()` 添加 null 检查 |

### 验证

```bash
# 注册新用户
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"manualtest2@example.com","password":"Test1234","tenantName":"ManualTest2"}'

# 用返回的token访问knowledge → code:0 成功
curl -s http://localhost:8080/api/v1/knowledge -H "Authorization: Bearer <token>"
# {"code":0,"data":{"id":12,"name":"默认知识库",...}}
```

---

## MT-DEF-02: 被邀请成员无法登录

### 现象

管理员邀请成员成功，但被邀请人使用自己的邮箱登录时，无论输入什么密码都无法登录成功。

### 根因分析

`TenantServiceImpl.inviteMember()` 方法为被邀请人生成了随机 UUID 密码：

```java
String randomPassword = UUID.randomUUID().toString().substring(0, 12);
user.setPasswordHash(passwordEncoder.encode(randomPassword));
```

但这个随机密码：
- 没有通过 API 响应返回给管理员
- 没有通过邮件发送给被邀请人
- `MemberResponse` DTO 不包含密码字段

**结果**：密码生成后即丢失，没有任何人知道它，被邀请人无法登录。

### 修复方案

在 `MemberResponse` DTO 中新增 `initialPassword` 字段（使用 `@JsonInclude(NON_NULL)` 避免在列表查询时暴露），邀请成功后将生成的密码通过 API 响应返回给管理员，管理员可手动告知被邀请人。

### 修改文件

| 文件 | 变更 |
|------|------|
| `module_tenant/dto/MemberResponse.java` | 新增 `initialPassword` 字段 |
| `module_tenant/service/impl/TenantServiceImpl.java` | `inviteMember()` 中调用 `response.setInitialPassword(initialPassword)` |

### 安全说明

`R<T>` 使用了 `@JsonInclude(JsonInclude.Include.NON_NULL)`，因此 `listMembers` 等其他接口返回的 `MemberResponse` 中 `initialPassword` 为 null 时不会出现在 JSON 中。密码仅在邀请接口响应中暴露给管理员。

---

## MT-DEF-03: 上传 3.12MB 文件返回 413 Request Entity Too Large

### 现象

上传一个 3.12MB 的 .doc 文件时，nginx 返回 `413 Request Entity Too Large`，错误页面显示 `nginx/1.31.2`。

### 根因分析

`docker/nginx.conf` 缺少 `client_max_body_size` 配置，nginx 默认值为 **1MB**。3.12MB 文件超限被 nginx 直接拒绝返回 413，请求未到达 Spring Boot。

> 注意：.doc 文件不在白名单（仅支持 PDF/MD/TXT）中，修复后上传 .doc 文件会正确返回 415（不支持的文件类型）而非 413。

### 修复方案

在 `docker/nginx.conf` 的 server 块中添加 `client_max_body_size 50m;`，与后端 Spring Boot 的 50MB multipart 限制保持一致。

### 修改文件

| 文件 | 变更 |
|------|------|
| `docker/nginx.conf` | server 块中添加 `client_max_body_size 50m;` |

---

## MT-DEF-04: GET /api/v1/widget/embed-script 返回 400 "聊天组件配置不存在"

### 现象

新注册用户进入聊天组件页面时，`GET /api/v1/widget/embed-script` 返回：

```json
{"code": 40702, "msg": "聊天组件配置不存在"}
```

嵌入脚本代码不可见，阻塞后续功能验证。

### 根因分析

新用户注册时，`AuthServiceImpl.register()` 只创建了 `Tenant` 和 `User`，**没有创建 `WidgetConfig`**。

`WidgetServiceImpl` 中存在不一致的模式：
- `updateConfig()` 使用了 `orElseGet(() -> createDefaultConfig(tenantId))` 懒创建兜底 ✅
- `getEmbedScript()` 使用了 `orElseThrow(() -> WIDGET_NOT_FOUND)` 直接抛异常 ❌
- `regenerateToken()` 同样直接抛异常 ❌

用户注册后直接请求嵌入脚本必然 400。

### 修复方案

**双重保障**：

1. **主路径**：注册流程中通过 Spring 事件机制自动创建 WidgetConfig
   - 新建 `TenantCreatedEvent` 事件类（放在 common 包）
   - 新建 `WidgetConfigInitializer` 监听器（放在 module_widget 包）
   - `AuthServiceImpl.register()` 中发布 `TenantCreatedEvent`
   - 使用 `@TransactionalEventListener(phase = AFTER_COMMIT)` 确保注册事务提交后才创建

2. **防御路径**：`getEmbedScript()` 和 `regenerateToken()` 增加懒创建兜底
   - 将 `orElseThrow` 改为 `orElseGet(() -> widgetConfigRepository.save(createDefaultConfig(tenantId)))`
   - 添加 `@Transactional` 注解确保事务一致性

### 修改文件

| 文件 | 变更 |
|------|------|
| `common/event/TenantCreatedEvent.java` | **新建**：租户创建领域事件 |
| `module_widget/listener/WidgetConfigInitializer.java` | **新建**：事件监听器，自动创建 WidgetConfig |
| `module_tenant/service/AuthServiceImpl.java` | 注入 `ApplicationEventPublisher`，注册后发布 `TenantCreatedEvent` |
| `module_widget/service/WidgetServiceImpl.java` | `getEmbedScript()` 和 `regenerateToken()` 改为懒创建模式 |

### 验证

```bash
# 注册新用户后访问widget embed-script → code:0 成功
curl -s http://localhost:8080/api/v1/widget/embed-script -H "Authorization: Bearer <token>"
# {"code":0,"data":{"script":"<script src=\"...\" data-token=\"...\"></script>",...}}
```

---

## 修复后构建与部署

```bash
# 1. 重新构建后端
cd server && mvn clean package -DskipTests -q

# 2. 重新构建Docker镜像
docker compose build server web --quiet

# 3. 重启服务
docker compose up -d server web

# 4. 验证启动
docker compose logs server --tail 5 | grep "Started"
# → Started DocChatApplication in 5.799 seconds
```

## 待回归验证

以下用户故事被 MT-DEF-03/04 阻塞，修复后需重新验证：

| 用户故事 | 阻塞原因 | 待验证内容 |
|----------|---------|-----------|
| US-004 上传文档 | 413错误 → 文件无法上传 | 上传 PDF/MD/TXT 文件（≤50MB） |
| US-005 文档列表 | 被US-004阻塞 | 文档列表展示、搜索、筛选 |
| US-006 删除文档 | 被US-004阻塞 | 删除确认、级联清理 |
| US-007 嵌入脚本 | 400错误 → 脚本不可见 | 嵌入脚本展示和复制 |
| US-010 版本历史 | 被US-004阻塞 | 版本列表和回滚 |
| US-011 对话问答 | 被US-004阻塞 | SSE流式响应（Embedding/LLM为占位） |
| E2E-01 完整旅程 | 多处阻塞 | 全流程端到端 |

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-25 | 第一轮人工测试缺陷定位修复记录，4个P0缺陷已修复 |
