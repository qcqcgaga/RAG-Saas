# 集成测试用例

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24
> 环境：Windows 11 + Java 21 + Spring Boot 3.3.6 + H2 内存数据库

## 用例列表

### 认证模块 (AuthControllerIT)

| 用例ID | 模块 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 状态 |
|--------|------|---------|---------|---------|---------|--------|------|
| TC-IT-001 | 认证 | 正常注册 | 无 | POST /api/v1/auth/register 邮箱+密码+租户名 | code=0, 返回userId/tenantId/role=ADMIN/token | P0 | ✅ 通过 |
| TC-IT-002 | 认证 | 重复邮箱注册 | 已注册同一邮箱 | POST /api/v1/auth/register 重复邮箱 | code=40104, msg=邮箱已注册 | P0 | ✅ 通过 |
| TC-IT-003 | 认证 | 密码强度不足 | 无 | POST /api/v1/auth/register 纯数字密码 | code=40000, 参数校验失败 | P1 | ✅ 通过 |
| TC-IT-004 | 认证 | 邮箱格式错误 | 无 | POST /api/v1/auth/register 非法邮箱 | code=40000, 参数校验失败 | P1 | ✅ 通过 |
| TC-IT-005 | 认证 | 空请求体 | 无 | POST /api/v1/auth/register {} | code=40000, 参数校验失败 | P1 | ✅ 通过 |
| TC-IT-006 | 认证 | 正常登录 | 已注册用户 | POST /api/v1/auth/login 邮箱+密码 | code=0, 返回JWT Token | P0 | ✅ 通过 |
| TC-IT-007 | 认证 | 密码错误 | 已注册用户 | POST /api/v1/auth/login 错误密码 | code=40101, Redis失败计数+1 | P0 | ✅ 通过 |
| TC-IT-008 | 认证 | 邮箱不存在 | 无 | POST /api/v1/auth/login 未注册邮箱 | code=40101 | P0 | ✅ 通过 |
| TC-IT-009 | 认证 | 账户锁定 | Redis计数>=5 | POST /api/v1/auth/login | code=40102, 账户已锁定 | P0 | ✅ 通过 |
| TC-IT-010 | 认证 | 账户禁用 | DB中status=0 | POST /api/v1/auth/login | code=40103, 账户已禁用 | P0 | ✅ 通过 |
| TC-IT-011 | 认证 | 登录成功清除失败计数 | 已注册+有失败记录 | POST /api/v1/auth/login 正确密码 | code=0, Redis删除失败key | P1 | ✅ 通过 |
| TC-IT-012 | 认证 | 注册→访问受保护API | 无 | 注册→提取Token→GET /tenants/current | code=0, 返回租户名称 | P0 | ✅ 通过 |

### 租户模块 (TenantControllerIT)

| 用例ID | 模块 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 状态 |
|--------|------|---------|---------|---------|---------|--------|------|
| TC-IT-013 | 租户 | 获取当前租户信息 | JWT认证 | GET /api/v1/tenants/current | 返回租户名称/slug/status | P0 | ✅ 通过 |
| TC-IT-014 | 租户 | 未认证访问 | 无Token | GET /api/v1/tenants/current | 403 Forbidden | P0 | ✅ 通过 |
| TC-IT-015 | 租户 | 管理员更新租户名称 | JWT ADMIN | PUT /api/v1/tenants/current | 返回更新后的名称 | P0 | ✅ 通过 |
| TC-IT-016 | 租户 | 邀请成员 | JWT ADMIN | POST /api/v1/tenants/members | 创建新用户,返回邮箱/角色 | P0 | ✅ 通过 |
| TC-IT-017 | 租户 | 邀请重复邮箱 | 成员已存在 | POST /api/v1/tenants/members | code=40301 | P1 | ✅ 通过 |
| TC-IT-018 | 租户 | 成员列表 | 有2个成员 | GET /api/v1/tenants/members | total=2 | P0 | ✅ 通过 |
| TC-IT-019 | 租户 | 修改成员角色 | JWT ADMIN | PUT /tenants/members/{id}/role | 角色更新成功 | P0 | ✅ 通过 |
| TC-IT-020 | 租户 | 移除成员 | JWT ADMIN | DELETE /tenants/members/{id} | 成员被删除 | P0 | ✅ 通过 |
| TC-IT-021 | 租户 | 不能移除自己 | JWT ADMIN | DELETE /tenants/members/自己ID | code=40300 | P1 | ✅ 通过 |
| TC-IT-022 | 租户 | 多租户成员列表隔离 | 两个租户 | A查成员列表 | 不含B的成员 | P0 | ✅ 通过 |
| TC-IT-023 | 租户 | 跨租户修改角色 | 两个租户 | A尝试修改B成员角色 | code=40300 | P0 | ✅ 通过 |
| TC-IT-024 | 租户 | 获取各自租户信息 | 两个租户 | A/B各自获取租户 | 返回各自的信息 | P0 | ✅ 通过 |

### 知识库模块 (KnowledgeControllerIT)

| 用例ID | 模块 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 状态 |
|--------|------|---------|---------|---------|---------|--------|------|
| TC-IT-025 | 知识库 | 首次获取自动创建 | JWT认证 | GET /api/v1/knowledge | 返回"默认知识库" | P0 | ✅ 通过 |
| TC-IT-026 | 知识库 | 更新知识库名称和描述 | 知识库已创建 | PUT /api/v1/knowledge | 返回更新后的名称/描述 | P0 | ✅ 通过 |
| TC-IT-027 | 知识库 | 上传TXT文件 | JWT认证 | POST /api/v1/knowledge/documents | 返回documentId/taskId | P0 | ✅ 通过 |
| TC-IT-028 | 知识库 | 上传不支持类型 | JWT认证 | POST 上传.exe文件 | code=40401 | P0 | ✅ 通过 |
| TC-IT-029 | 知识库 | 空知识库列表 | 无文档 | GET /api/v1/knowledge/documents | total=0 | P1 | ✅ 通过 |
| TC-IT-030 | 知识库 | 上传后查询列表 | 有1个文档 | GET /api/v1/knowledge/documents | total=1 | P0 | ✅ 通过 |
| TC-IT-031 | 知识库 | 分页查询 | 有2个文档 | GET /api/v1/knowledge/documents?page=1&size=1 | list.length=1, total=2 | P1 | ✅ 通过 |
| TC-IT-032 | 知识库 | 未确认删除 | 有文档 | DELETE /documents/{id} | code=40000 | P0 | ✅ 通过 |
| TC-IT-033 | 知识库 | 确认删除 | 有文档 | DELETE /documents/{id}?confirm=true | 文档删除+触发DELETE_VECTORS | P0 | ✅ 通过 |
| TC-IT-034 | 知识库 | 多租户文档列表隔离 | 两个租户各1个文档 | A查文档列表 | 仅A的文档 | P0 | ✅ 通过 |
| TC-IT-035 | 知识库 | 跨租户删除文档 | 两个租户 | A尝试删B的文档 | code=40300 | P0 | ✅ 通过 |

### 任务模块 (TaskControllerIT)

| 用例ID | 模块 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 状态 |
|--------|------|---------|---------|---------|---------|--------|------|
| TC-IT-036 | 任务 | 获取任务列表 | 有1个FAILED任务 | GET /api/v1/tasks | total=1 | P0 | ✅ 通过 |
| TC-IT-037 | 任务 | 获取任务详情 | 任务存在 | GET /api/v1/tasks/{id} | 返回完整任务信息 | P0 | ✅ 通过 |
| TC-IT-038 | 任务 | 任务不存在 | 无 | GET /api/v1/tasks/99999 | code=40500 | P1 | ✅ 通过 |
| TC-IT-039 | 任务 | 重试FAILED任务 | 有FAILED任务 | POST /api/v1/tasks/{id}/retry | status=PENDING, retryCount+1 | P0 | ✅ 通过 |
| TC-IT-040 | 任务 | 重试非FAILED任务 | 有COMPLETED任务 | POST /api/v1/tasks/{id}/retry | code=40501 | P1 | ✅ 通过 |
| TC-IT-041 | 任务 | 超过最大重试次数 | retryCount=3 | POST /api/v1/tasks/{id}/retry | code=40502 | P1 | ✅ 通过 |
| TC-IT-042 | 任务 | 跨租户查看任务 | 两个租户 | B查看A的任务 | code=40300 | P0 | ✅ 通过 |
| TC-IT-043 | 任务 | 跨租户重试任务 | 两个租户 | B重试A的任务 | code=40300 | P0 | ✅ 通过 |
| TC-IT-044 | 任务 | 多租户任务列表隔离 | 两个租户 | B查任务列表 | total=0 | P0 | ✅ 通过 |

### 聊天组件模块 (WidgetControllerIT)

| 用例ID | 模块 | 测试场景 | 前置条件 | 操作步骤 | 预期结果 | 优先级 | 状态 |
|--------|------|---------|---------|---------|---------|--------|------|
| TC-IT-045 | Widget | 有效Token获取配置 | 有WidgetConfig | GET /api/v1/widget/config?token=xxx | 返回brandColor/welcomeMessage/enabled | P0 | ✅ 通过 |
| TC-IT-046 | Widget | 无效Token获取配置 | 无 | GET /api/v1/widget/config?token=invalid | code=40701 | P0 | ✅ 通过 |
| TC-IT-047 | Widget | 更新品牌色和欢迎语 | JWT ADMIN | PUT /api/v1/widget/config | 返回更新后的配置 | P0 | ✅ 通过 |
| TC-IT-048 | Widget | 禁用组件 | JWT ADMIN | PUT /api/v1/widget/config enabled=0 | enabled=0 | P1 | ✅ 通过 |
| TC-IT-049 | Widget | 获取嵌入脚本 | 有WidgetConfig | GET /api/v1/widget/embed-script | 返回script和previewUrl | P0 | ✅ 通过 |
| TC-IT-050 | Widget | 无配置获取嵌入脚本 | 无WidgetConfig | GET /api/v1/widget/embed-script | code=40702 | P1 | ✅ 通过 |
| TC-IT-051 | Widget | 管理员重新生成Token | JWT ADMIN | POST /api/v1/widget/regenerate-token | 返回新Token | P0 | ✅ 通过 |
| TC-IT-052 | Widget | 成员无权重新生成Token | JWT MEMBER | POST /api/v1/widget/regenerate-token | code=40300 | P1 | ✅ 通过 |

## 跨模块集成验证

| 用例ID | 场景 | 验证内容 | 优先级 | 状态 |
|--------|------|---------|--------|------|
| TC-IT-053 | 注册→访问受保护API | JWT Token全链路：生成→解析→SecurityContext→TenantContext | P0 | ✅ 通过 |

## 状态定义

| 状态 | 说明 |
|------|------|
| ✅ 通过 | 测试执行，结果符合预期 |
| ❌ 失败 | 测试执行，结果不符合预期 |
| ⬜ 待执行 | 未开始测试 |
