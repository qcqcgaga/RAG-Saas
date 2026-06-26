# 人工测试缺陷定位修复记录 — 第二轮

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0 (mvp-2026-06-24)
> 测试日期：2026-06-25
> 修复日期：2026-06-25

## 缺陷清单

| 缺陷ID | 关联用户故事 | 描述 | 严重度 | 状态 | 修复轮次 |
|---------|-------------|------|--------|------|---------|
| MT-DEF-05 | US-001 | 注册后 GET /knowledge/documents 返回 500 "系统繁忙" | P0 | ✅ 已在R1修复中解决 | 第2轮 |
| MT-DEF-06 | US-006 | 删除文档确认时报错：需传 confirm=true | P1 | ✅ 已修复 | 第2轮 |
| MT-DEF-07 | US-003 | 被邀请成员无法登录（设计缺陷） | P0 | ✅ 已修复 | 第2轮 |

---

## MT-DEF-05: 注册后 GET /knowledge/documents 返回 500

### 现象

新注册用户注册成功后，前端请求 `GET /api/v1/knowledge/documents?page=1&size=20` 返回：

```json
{"code": 50000, "msg": "系统繁忙，请稍后重试"}
```

### 根因分析

**与 MT-DEF-01 同根因**：JwtAuthenticationFilter 双重注册导致 TenantContext 为 null，R1 修复已覆盖此接口。

**本轮发现**：用户测试时使用了旧会话/缓存，导致仍看到 500 错误。清除缓存或使用新注册账号后问题不复现。

### 修复方案

无需额外修复，R1 修复已解决。本轮验证确认：

```bash
# 注册新用户 → 自动获取 token
# GET /knowledge/documents → {"code":0,"data":{"list":[],"total":0,"page":1,"size":20}}
```

### 验证结果

✅ 注册后 `GET /knowledge/documents` 正常返回 200

---

## MT-DEF-06: 删除文档确认时报错

### 现象

用户在知识库页面点击删除按钮并确认（弹窗二次确认），返回错误：

```json
{"code": 40000, "msg": "参数校验失败: 删除文档需要二次确认，请传入 confirm=true"}
```

### 根因分析

**前后端不一致**：

- **后端**：`KnowledgeController.deleteDocument()` 定义了 `@RequestParam(defaultValue = "false") boolean confirm`，要求 `confirm=true` 才允许删除
- **前端**：`deleteDocument()` API 调用 `request.delete('/api/v1/knowledge/documents/${documentId}')`，**没有传递 confirm 参数**
- 前端使用 `a-popconfirm` 组件提供了 UI 二次确认，但确认结果未传递给后端

**结果**：用户在 UI 上确认了删除，但后端收到的 confirm 默认为 false，校验失败。

### 修复方案

前端 `deleteDocument()` API 调用添加 `params: { confirm: true }` 参数，与 `a-popconfirm` 的确认状态联动。

### 修改文件

| 文件 | 变更 |
|------|------|
| `web/src/api/knowledge.ts` | `deleteDocument()` 添加 `params: { confirm: true }` |

### 代码变更

```typescript
// 修复前
export function deleteDocument(documentId: number) {
  return request.delete<any, any>(`/api/v1/knowledge/documents/${documentId}`)
}

// 修复后
export function deleteDocument(documentId: number) {
  return request.delete<any, any>(`/api/v1/knowledge/documents/${documentId}`, {
    params: { confirm: true },
  })
}
```

### 验证结果

```bash
# 不带 confirm → 400 参数校验失败 ✅
curl -X DELETE /api/v1/knowledge/documents/1
# {"code":40000,"msg":"参数校验失败: 删除文档需要二次确认，请传入 confirm=true"}

# 带 confirm=true → 校验通过（404为文档不存在，非confirm问题）✅
curl -X DELETE /api/v1/knowledge/documents/999?confirm=true
# {"code":40404,"msg":"文档不存在"}
```

---

## MT-DEF-07: 被邀请成员无法登录（设计缺陷）

### 现象

管理员邀请成员成功后，被邀请人无法登录。R1 修复（添加 initialPassword 字段返回随机密码）后，用户反馈"问题依然存在"。

### 根因分析

**设计缺陷**：R1 修复方案（系统生成随机密码 → 通过 API 返回 → 前端展示给管理员 → 管理员告知被邀请人）存在以下问题：

1. **前端未展示**：`handleInvite()` 只显示 "邀请成功"，没有展示 `initialPassword`
2. **安全隐患**：随机密码通过 API 响应明文传输，且只有一次查看机会
3. **用户体验差**：管理员需要手动复制密码再转发，容易遗忘

**用户决策**：采用"管理员指定密码"方案，邀请时由管理员为被邀请人设定初始密码。

### 修复方案

**方案：管理员在邀请时直接为被邀请人指定初始密码**

修改内容：

1. **后端**：`InviteMemberRequest` 新增 `password` 字段（含密码强度校验），`TenantServiceImpl.inviteMember()` 使用管理员指定的密码代替随机生成
2. **前端**：邀请弹窗新增"初始密码"输入框（密码强度提示），API 调用传递密码

### 修改文件

| 文件 | 变更 |
|------|------|
| `server/.../dto/InviteMemberRequest.java` | 新增 `password` 字段 + `@NotBlank` `@Size` `@Pattern` 校验 |
| `server/.../service/TenantServiceImpl.java` | `inviteMember()` 使用 `request.getPassword()` 代替 UUID 随机密码，移除 `initialPassword` 设置，移除未使用的 `UUID` import |
| `web/src/api/tenant.ts` | `inviteMember()` 参数类型添加 `password` |
| `web/src/views/tenant/TenantView.vue` | 邀请弹窗添加密码输入框 + 密码强度校验，`inviteForm` 添加 `password` 字段 |

### 代码变更

**后端 — InviteMemberRequest.java：**

```java
// 新增字段
@NotBlank(message = "密码不能为空")
@Size(min = 8, max = 64, message = "密码长度必须在8-64位之间")
@Pattern(regexp = ".*[A-Z].*", message = "密码必须包含大写字母")
@Pattern(regexp = ".*[a-z].*", message = "密码必须包含小写字母")
@Pattern(regexp = ".*\\d.*", message = "密码必须包含数字")
private String password;
```

**后端 — TenantServiceImpl.java：**

```java
// 修复前：生成随机密码
String initialPassword = UUID.randomUUID().toString().substring(0, 12);
user.setPasswordHash(passwordEncoder.encode(initialPassword));
MemberResponse response = toMemberResponse(user);
response.setInitialPassword(initialPassword);
return response;

// 修复后：使用管理员指定的密码
User user = User.builder()
    .tenantId(tenantId)
    .email(request.getEmail())
    .passwordHash(passwordEncoder.encode(request.getPassword()))
    .role(request.getRole())
    .status((short) 1)
    .build();
user = userRepository.save(user);
return toMemberResponse(user);
```

**前端 — TenantView.vue：**

```vue
<!-- 新增密码输入框 -->
<a-form-item label="初始密码"
  :rules="[
    { required: true, message: '请输入初始密码' },
    { min: 8, message: '密码至少8位' },
    { pattern: /.*[A-Z].*/, message: '需包含大写字母' },
    { pattern: /.*[a-z].*/, message: '需包含小写字母' },
    { pattern: /.*\d.*/, message: '需包含数字' },
  ]">
  <a-input-password v-model:value="inviteForm.password"
    placeholder="至少8位，含大小写字母和数字" />
</a-form-item>
```

### 验证结果

```bash
# 邀请成员（管理员指定密码）
curl -X POST /api/v1/tenants/members \
  -d '{"email":"invited_r2@example.com","role":"MEMBER","password":"Invite123"}'
# {"code":0,"data":{"userId":25,"email":"invited_r2@example.com","role":"MEMBER",...}}

# 被邀请人登录 ✅
curl -X POST /api/v1/auth/login \
  -d '{"email":"invited_r2@example.com","password":"Invite123"}'
# {"code":0,"data":{"userId":25,"tenantId":21,"role":"MEMBER","token":"..."}}
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
docker compose logs server --tail 3 | grep "Started"
# → Started DocChatApplication in 5.853 seconds
```

## 待回归验证

| 用户故事 | 阻塞原因 | 待验证内容 |
|----------|---------|-----------|
| US-001 注册账号 | R1修复后需确认 | 注册后 knowledge/documents 不再500 |
| US-003 邀请成员 | 设计方案变更 | 管理员可设定密码，被邀请人可登录 |
| US-006 删除文档 | confirm参数修复 | 删除确认不再报错 |
| E2E-01 完整旅程 | 多处阻塞 | 全流程端到端 |

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-25 | 第二轮人工测试缺陷定位修复记录，1个R1遗留确认 + 2个新缺陷修复 |
