# 公共层 API

## 认证 API — AuthController

基础路径: `/api/v1/auth`

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/register` | 用户注册 | `RegisterRequest` | `R<AuthResponse>` |
| POST | `/login` | 用户登录 | `LoginRequest` | `R<AuthResponse>` |

### RegisterRequest
```java
{ email: String, password: String, tenantName: String, slug: String }
```

### LoginRequest
```java
{ email: String, password: String }
```

### AuthResponse
```java
{ token: String, userId: Long, email: String, tenantId: Long, role: String }
```

---

## 租户管理 API — TenantController

基础路径: `/api/v1/tenants`（需 JWT）

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/current` | 获取当前租户信息 | - | `R<TenantResponse>` |
| PUT | `/current` | 更新租户信息 | `UpdateTenantRequest` | `R<TenantResponse>` |
| GET | `/members` | 成员列表(分页) | query: page, size | `R<PageResult<MemberResponse>>` |
| POST | `/members` | 邀请成员 | `InviteMemberRequest` | `R<MemberResponse>` |
| PUT | `/members/{userId}/role` | 修改成员角色 | `UpdateRoleRequest` | `R<Void>` |
| DELETE | `/members/{userId}` | 移除成员 | - | `R<Void>` |

### 角色体系

| 角色 | 权限 |
|------|------|
| ADMIN | 全部操作 |
| MEMBER | 查看+管理知识库/组件 |
| READONLY | 仅查看 |
