# 公共层 (common)

> 跨模块共享的技术性代码，禁止放置业务逻辑。

## 目录结构

```
server/.../common/
├── config/          # 全局配置类
│   ├── SecurityConfig.java       # Spring Security 配置
│   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   ├── JwtProperties.java        # JWT 配置属性
│   ├── TenantContext.java        # 租户上下文 (ThreadLocal)
│   └── TenantFilter.java         # 租户 ID 自动注入过滤器
├── exception/       # 全局异常体系
│   ├── BaseException.java        # 异常基类
│   ├── BizException.java         # 业务异常 (可预期)
│   ├── SystemException.java      # 系统异常 (不可预期)
│   └── GlobalExceptionHandler.java  # @RestControllerAdvice 统一处理
├── response/        # 统一响应封装
│   ├── R.java                    # 统一响应 R<T>
│   ├── PageResult.java           # 分页结果
│   └── ErrorCode.java            # 错误码枚举
├── util/            # 工具类
│   ├── JwtUtil.java              # JWT 生成/解析
│   └── SecurityUtil.java         # 获取当前用户信息
└── constant/        # 全局常量
    └── CommonConstant.java       # 通用常量定义
```

## 核心机制

### 多租户隔离

- **TenantContext**：ThreadLocal 存储当前租户 ID
- **TenantFilter**：从 JWT 解析 tenant_id，写入 TenantContext
- **禁止**：Service 层手动传递 tenant_id 做过滤

### JWT 认证流程

```
请求 → TenantFilter(解析tenant_id) → JwtAuthenticationFilter(验证JWT)
     → SecurityContext(存入用户信息) → Controller → Service
```

### 统一响应格式

```java
R.ok(data)          → { "code": 0, "msg": "success", "data": {...} }
R.fail(errorCode)   → { "code": 40401, "msg": "文档不存在", "data": null }
R.ok(PageResult)    → { "code": 0, "msg": "success", "data": { "list": [...], "total": 100 } }
```

### 异常体系

```
BaseException
├── BizException    — 可预期业务错误，返回 4xx
└── SystemException — 不可预期系统错误，返回 5xx
```

## 详细文档

- [api-common.md](api-common.md) — 公共 API（认证/租户上下文）
- [data-model.md](data-model.md) — 公共数据结构
- [pitfalls.md](pitfalls.md) — 坑点和注意事项
