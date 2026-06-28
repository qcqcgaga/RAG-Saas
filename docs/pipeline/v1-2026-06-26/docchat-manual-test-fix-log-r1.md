# 人工测试缺陷定位修复记录 — 第1轮

> 项目：DocChat
> 版本：v1-2026-06-26
> 修复日期：2026-06-28
> 修复轮次：R1

## 缺陷总览

| # | 现象 | 根因 | 严重度 | 修复状态 |
|---|------|------|--------|----------|
| D1 | 刚注册后 `/api/v1/knowledge/documents` 报系统繁忙 | Controller中`SecurityUtil.getCurrentTenantId()`未做null检查 + 全局异常处理器掩盖真实错误 | P0 | ✅ 已修复 |
| D2 | `/api/v1/stats/overview` 报系统繁忙 | StatController中`getCurrentTenantId()`未做null检查，null传入JPA查询导致异常 | P0 | ✅ 已修复 |
| D3 | `/api/v1/eval-sets` 报系统繁忙 | **前端API路径与后端不匹配**：前端调用`/api/v1/eval-sets`，后端Controller路径是`/api/v1/eval/sets` | P0 | ✅ 已修复 |
| D4 | `/api/v1/llm-config` 报系统繁忙 | LlmConfigController中`getCurrentTenantId()`**硬编码返回1L** + 无权限校验 | P0 | ✅ 已修复 |
| D5 | LLM配置页面不应开放给普通用户 | 后端无ADMIN权限校验 + 前端菜单无角色控制 | P1 | ✅ 已修复 |

## 共同根因

**所有"系统繁忙"的共同根因是全局异常处理器把所有未预期异常（包括404路径不存在、401认证失败、403权限不足）都统一返回"系统繁忙，请稍后重试"，掩盖了真实错误信息。** 前端无法区分是路径错误、认证失败还是真正的服务器错误。

## 缺陷详情与修复

### D1: 刚注册后知识库文档接口报系统繁忙

**现象**：刚注册账号登录时，`/api/v1/knowledge/documents?page=1&size=20` 报系统繁忙。之后重新登录首次加载页面不会出现此问题。

**根因分析**：
1. `SecurityUtil.getCurrentTenantId()` 从 `TenantContext`（ThreadLocal）获取tenantId
2. TenantContext由TenantFilter从JWT token解析设置
3. 如果Filter执行时序异常或token解析失败，tenantId可能为null
4. KnowledgeServiceImpl中有null检查（抛BizException 400），但其他Controller没有
5. 全局异常处理器把所有异常兜底返回"系统繁忙"

**修复方案**：
- 所有Controller统一使用`requireCurrentTenantId()`方法，null时抛BizException(UNAUTHORIZED)
- 改进全局异常处理器，区分404/401/403/500
- 改进前端错误处理，区分不同HTTP状态码

**修改文件**：
- `server/.../common/exception/GlobalExceptionHandler.java` — 增加404/403处理器
- `server/.../common/config/SecurityConfig.java` — 增加401/403的R格式响应
- `web/src/utils/request.ts` — 区分401/403/404/500错误提示

### D2: 用量统计接口报系统繁忙

**现象**：点击用量统计页签，`/api/v1/stats/overview?period=7d` 报系统繁忙

**根因分析**：
1. StatController中3处调用`SecurityUtil.getCurrentTenantId()`**均未做null检查**
2. tenantId=null传入`statService.getOverview(null, period)`
3. JPQL查询`WHERE l.tenantId = :tenantId`中tenantId=null导致JPA抛异常
4. 异常被全局异常处理器兜底返回"系统繁忙"

**修复方案**：
- StatController新增`requireCurrentTenantId()`私有方法，null时抛BizException
- 所有3处调用改为使用`requireCurrentTenantId()`

**修改文件**：
- `server/.../module_stat/controller/StatController.java`

### D3: 评测集接口报系统繁忙

**现象**：点击评测集页签，`/api/v1/eval-sets` 报系统繁忙

**根因分析**：
1. **前端API路径与后端Controller路径不匹配**
2. 前端`eval.ts`调用`/api/v1/eval-sets`（连字符风格）
3. 后端`EvalController`路径是`/api/v1/eval/sets`（路径层级风格）
4. Spring MVC找不到`/api/v1/eval-sets`对应的Handler，返回404
5. 404被全局异常处理器兜底返回"系统繁忙"
6. **更严重的是**：前端eval.ts中所有API路径（创建/删除/评测对/执行/结果）都与后端不匹配

**修复方案**：
- 前端`eval.ts`中所有API路径对齐后端Controller路径
- `/api/v1/eval-sets` → `/api/v1/eval/sets`
- `/api/v1/eval-sets/{id}/pairs/batch` → `/api/v1/eval/sets/{id}/pairs/import`
- `/api/v1/eval-sets/{id}/execute` → `/api/v1/eval/sets/{id}/run`
- EvalController也增加`requireCurrentTenantId()`null检查

**修改文件**：
- `web/src/api/eval.ts` — 修正所有API路径
- `server/.../module_eval/controller/EvalController.java` — 增加null检查

### D4: LLM配置接口报系统繁忙

**现象**：点击LLM配置，`/api/v1/llm-config` 报系统繁忙

**根因分析**：
1. **LlmConfigController中`getCurrentTenantId()`硬编码返回`1L`**
2. 这意味着所有租户共享tenantId=1的LLM配置，严重违反多租户隔离
3. 硬编码导致非tenantId=1的租户无法正确获取自己的LLM配置
4. 同时该Controller没有任何权限校验（注释说"仅管理员可操作"但未实现）

**修复方案**：
- `getCurrentTenantId()`改为从`SecurityUtil.getCurrentTenantId()`获取
- 新增`requireAdminTenantId()`方法：先校验ADMIN角色，再获取tenantId
- 所有接口方法使用`requireAdminTenantId()`

**修改文件**：
- `server/.../module_chat/controller/LlmConfigController.java`

### D5: LLM配置页面权限问题

**现象**：LLM配置页面对所有租户用户开放，不应如此

**根因分析**：
1. 后端LlmConfigController没有ADMIN权限校验（只有注释"仅管理员可操作"）
2. 前端AppLayout菜单中LLM配置对所有登录用户可见
3. 前端路由没有权限守卫，可通过URL直接访问
4. **用户之前提供的API KEY配置在`application.yml`中**（`docchat.llm.api-key`环境变量`LLM_API_KEY`）
5. LLM配置页面是用于**租户级覆盖系统默认配置**的，不是替代application.yml

**修复方案**：
- 后端：LlmConfigController所有接口增加ADMIN角色校验
- 前端：AppLayout菜单中LLM配置项加`v-if="userStore.isAdmin"`条件
- 前端：路由守卫增加`requiresAdmin`meta检查
- 前端：userStore中role持久化到localStorage（页面刷新后不丢失）
- 前端：LLM配置页面测试连通性需传请求体参数

**修改文件**：
- `server/.../module_chat/controller/LlmConfigController.java`
- `web/src/components/layout/AppLayout.vue`
- `web/src/router/index.ts`
- `web/src/stores/user.ts`
- `web/src/api/llmConfig.ts`
- `web/src/views/llm-config/index.vue`

## 其他修复

### ApiKeyController null检查

- `ApiKeyController.getCurrentTenantId()`可能返回null，增加`requireCurrentTenantId()`方法

**修改文件**：`server/.../module_apikey/controller/ApiKeyController.java`

### 单元测试修复

- `ApiKeyServiceImplTest`：`setStatus(int)` → `setStatus((short) int)`，`isEqualTo(0)` → `isEqualTo((short) 0)`
- `LlmServiceTest`：移除需要真实LLM API的测试，改为测试配置解析逻辑；修复`verifyNoInteraction` → `verifyNoInteractions`

**修改文件**：
- `server/.../module_apikey/service/ApiKeyServiceImplTest.java`
- `server/.../module_chat/service/LlmServiceTest.java`

## 验证结果

- ✅ 后端编译通过（`mvn compile`）
- ✅ 后端146个单元测试全部通过（`mvn test`）
- ✅ 前端类型检查通过（`vue-tsc --noEmit`）

## 经验教训

1. **全局异常处理器不能掩盖真实错误**：把404/401/403/500统一返回"系统繁忙"是最危险的做法，它让所有调试线索消失，开发者和用户都无法判断问题所在
2. **前后端API路径必须对齐**：前端写API路径时必须对照后端Controller的`@RequestMapping`，不能凭直觉命名。连字符风格(`eval-sets`)和路径层级风格(`eval/sets`)极易混淆
3. **Controller中getCurrentTenantId()必须做null检查**：SecurityUtil从ThreadLocal获取tenantId，任何Filter执行时序问题都可能导致null。统一使用`requireCurrentTenantId()`模式
4. **硬编码是定时炸弹**：`getCurrentTenantId() { return 1L; }`这种临时方案必须立即替换，否则多租户隔离形同虚设
5. **权限校验不能只写在注释里**：`// 仅管理员可操作`不等于真的只有管理员能操作，必须在代码中实现
6. **前端角色信息需要持久化**：Pinia store在页面刷新后丢失，role必须存到localStorage
7. **LLM配置的定位要明确**：application.yml中的`docchat.llm.api-key`是系统默认配置，LLM配置页面是租户级覆盖，两者互补
