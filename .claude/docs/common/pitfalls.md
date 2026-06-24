# 公共层坑点

## 1. TenantContext 线程安全

TenantContext 使用 ThreadLocal，**异步线程中会丢失**。
- TaskWorker 等异步场景需手动传递 tenant_id
- `@Async` 方法中 TenantContext.getTenantId() 返回 null

## 2. JWT 过期与刷新

当前 JWT 无刷新机制，过期后需重新登录。
- `JWT_EXPIRATION` 默认 86400 秒(24小时)
- 前端需在 401 响应时跳转登录页

## 3. SecurityConfig 放行路径

`/api/chat/**` 和 `/api/widget/**` 放行（widget_token 鉴权，不走 JWT）。
新增公开 API 时需同步更新 SecurityConfig。

## 4. 异常体系使用

- **BizException**: 可预期的业务错误，如"文档不存在"、"邮箱已注册"
- **SystemException**: 不可预期的系统错误，如"数据库连接失败"
- 不要用 SystemException 代替 BizException，前者会触发告警

## 5. R<T> 响应规范

- Controller 层统一返回 `R<T>`
- 成功: `R.ok(data)` / `R.ok()`
- 失败: `R.fail(ErrorCode.XXX)`
- 禁止直接抛异常让 GlobalExceptionHandler 包装业务逻辑
