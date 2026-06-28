# DocChat 运维指导

> 本文档面向 DocChat SaaS 平台的日常运维和故障排查，持续扩充。
>
> 最后更新：2026-06-28

---

## 目录

1. [服务架构与端口](#1-服务架构与端口)
2. [启停与重建](#2-启停与重建)
3. [健康检查](#3-健康检查)
4. [日志查看](#4-日志查看)
5. [数据库运维](#5-数据库运维)
6. [常见故障排查](#6-常见故障排查)
7. [部署检查清单](#7-部署检查清单)
8. [配置管理](#8-配置管理)

---

## 1. 服务架构与端口

```
                    ┌─────────────────────────────────────────┐
                    │           Docker Compose                │
                    │                                         │
  :80  ◄───────────│  web (Nginx)                            │
  前端页面          │    ├─ /          → Vue SPA              │
  静态资源          │    ├─ /api/*     → proxy to server:8080 │
                    │    ├─ /widget.js → 静态文件              │
                    │    └─ /widget.css → 静态文件             │
                    │                                         │
  :8080 ◄──────────│  server (Spring Boot)                   │
  后端API           │    ├─ /api/v1/auth/**     (permitAll)  │
  静态资源          │    ├─ /api/v1/chat/**     (permitAll)  │
                    │    ├─ /api/v1/widget/**   (permitAll)  │
                    │    ├─ /widget.js,widget.css (permitAll) │
                    │    └─ /api/v1/**其他      (需JWT认证)   │
                    │                                         │
  :5432 ◄──────────│  postgres                               │
                    │                                         │
  :6379 ◄──────────│  redis                                  │
                    │                                         │
  :19530 ◄─────────│  milvus                                  │
                    │                                         │
  :5050 ◄──────────│  pgadmin                                 │
                    └─────────────────────────────────────────┘
```

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| Nginx 前端 | rag-saas-web-1 | :80 | Vue SPA + API 反向代理 + 静态资源 |
| Spring Boot | rag-saas-server-1 | :8080 | 后端 API + widget 静态资源 |
| PostgreSQL | rag-saas-postgres-1 | :5432 | 主数据库 |
| Redis | rag-saas-redis-1 | :6379 | 缓存 / 限流计数 / 评测锁 |
| Milvus | rag-saas-milvus-1 | :19530 | 向量数据库 |
| etcd | rag-saas-etcd-1 | :2379 | Milvus 元数据存储 |
| MinIO | rag-saas-minio-1 | :9000/:9001 | Milvus 对象存储 |
| pgAdmin | rag-saas-pgadmin-1 | :5050 | PostgreSQL 管理界面 |

---

## 2. 启停与重建

### 2.1 全量启动（推荐）

```bash
# 前提：后端 JAR 和前端 dist 已构建
cd d:/agent_project/RAG-Saas

# 1. 构建产物
cd server && mvn package -DskipTests -q && cd ..
cd web && pnpm build && cd ..
cd packages/chat-widget && npx vite build && cd ..

# 2. 复制 chat-widget 产物到后端静态目录
cp packages/chat-widget/dist/widget.js server/src/main/resources/static/widget.js
cp packages/chat-widget/dist/widget.css server/src/main/resources/static/widget.css

# 3. 启动所有服务（含镜像构建）
docker compose up -d --build
```

### 2.2 仅重建后端

```bash
# 修改了 Java 代码后
cd server && mvn package -DskipTests -q && cd ..
docker compose up -d --build server
```

### 2.3 仅重建前端

```bash
# 修改了 Vue 代码后
cd web && pnpm build && cd ..
docker compose up -d --build web
```

### 2.4 停止所有服务

```bash
docker compose down
```

### 2.5 停止并清除数据卷

```bash
# ⚠️ 会删除所有持久化数据（数据库、上传文件、向量索引等）
docker compose down -v
```

### 2.6 关键注意事项

> **⚠️ 代码修改后必须重新构建再重启，否则 Docker 容器运行的仍是旧代码。**

修复代码但不重建容器 = 修复无效。这是人工测试中反复出现的问题：
修改了代码但忘了重启 Docker，导致人工测试仍然复现旧 Bug。

正确的修改→验证流程：
1. 修改源代码
2. 重新构建（`mvn package` / `pnpm build` / `vite build`）
3. 重建并重启容器（`docker compose up -d --build <service>`）
4. 等待服务完全启动（后端约 30 秒）
5. 执行验证

---

## 3. 健康检查

### 3.1 查看所有服务状态

```bash
docker compose ps
```

重点关注 `STATUS` 列是否为 `healthy`。后端没有 Docker 层面的 healthcheck，需手动验证。

### 3.2 快速验证后端启动

```bash
# 返回 401（业务错误）= 服务已启动
curl -s http://localhost:8080/api/v1/eval/sets | head -1
```

### 3.3 快速验证前端 + Nginx 代理

```bash
# 应返回 {"code":40101,"msg":"邮箱或密码错误"}
curl -s http://localhost/api/v1/auth/login \
  -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"wrong"}'
```

### 3.4 验证 Milvus 连通性

```bash
curl -s http://localhost:9091/healthz
```

---

## 4. 日志查看

### 4.1 查看后端日志

```bash
# 最近 50 行
docker compose logs server --tail 50

# 实时跟踪
docker compose logs server -f

# 只看错误和警告
docker compose logs server --tail 200 | grep -E "(ERROR|WARN)"

# 搜索特定关键词
docker compose logs server --tail 500 | grep "ChatStatAspect"
```

### 4.2 查看特定服务日志

```bash
docker compose logs postgres --tail 30
docker compose logs redis --tail 30
docker compose logs milvus --tail 30
docker compose logs web --tail 30
```

### 4.3 关键日志关键词

| 关键词 | 含义 | 严重度 |
|--------|------|--------|
| `Started DocChatApplication` | 后端启动成功 | ℹ️ |
| `业务异常: [40101]` | 登录失败（正常） | ℹ️ |
| `聚合结果转long失败` | 统计查询类型转换失败 | ⚠️ 需修复 |
| `资源不存在:` | 前端请求了不存在的 API 路径 | ⚠️ 路径对齐问题 |
| `ChatStatAspect: authType=` | 用量统计切面触发 | ℹ️ 调试用 |
| `记录用量: tenantId=` | 用量记录成功 | ℹ️ 调试用 |
| `用量记录失败` | 统计 AOP 异常 | 🔴 需修复 |
| `评测虚拟线程异常` | 评测执行失败 | 🔴 需修复 |
| `未预期异常` | 全局异常兜底 | 🔴 需修复 |

---

## 5. 数据库运维

### 5.1 连接数据库

```bash
docker exec -it rag-saas-postgres-1 psql -U docchat -d docchat
```

### 5.2 常用查询

```bash
# 查看所有用户
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, email, role, tenant_id, status FROM users ORDER BY id;"

# 查看用量统计日志
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, tenant_id, auth_type, prompt_tokens, completion_tokens, created_at FROM chat_usage_logs ORDER BY id DESC LIMIT 20;"

# 查看评测集
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, tenant_id, name, pair_count FROM eval_sets ORDER BY id;"

# 查看评测结果
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, eval_set_id, status, hit_rate, hit_count, total_pairs FROM eval_results ORDER BY id DESC;"

# 查看 Widget 配置
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, tenant_id, widget_token, enabled, brand_color FROM widget_configs;"

# 查看 API Key
docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
  -c "SELECT id, tenant_id, name, status FROM api_keys ORDER BY id;"
```

### 5.3 验证 native query 返回值

```bash
# 直接验证统计聚合查询（绕过 Hibernate）
docker exec rag-saas-postgres-1 psql -U docchat -d docchat -c "
SELECT COUNT(*) AS calls,
       COALESCE(SUM(prompt_tokens), 0) AS pt,
       COALESCE(SUM(completion_tokens), 0) AS ct,
       COALESCE(SUM(total_tokens), 0) AS tt
FROM chat_usage_logs
WHERE tenant_id = 28
  AND auth_type <> 'JWT'
  AND created_at >= '2026-06-21' AND created_at < '2026-06-29';
"
```

---

## 6. 常见故障排查

### 6.1 前端 API 报 404 "请求的资源不存在"

**症状**：浏览器控制台或后端日志出现 `资源不存在: api/v1/eval-sets`

**排查步骤**：

1. **确认路径格式**：前端 API 路径必须与后端 `@RequestMapping` 对齐
   - ❌ `/api/v1/eval-sets`（连字符风格）
   - ✅ `/api/v1/eval/sets`（路径层级风格）

2. **检查浏览器缓存**：强制刷新（Ctrl+Shift+R）或清除缓存，旧版 JS 可能被缓存

3. **检查 Nginx 代理**：确认 `/api/` 请求被正确代理到后端
   ```bash
   curl -s http://localhost/api/v1/eval/sets | head -1
   # 应返回 401 的 JSON 响应，而不是 404
   ```

4. **检查后端 Controller 路径**：
   ```bash
   grep -r "RequestMapping" server/src/main/java/com/docchat/module_eval/controller/
   ```

**根因模式**：连字符风格 (`eval-sets`) 和路径层级风格 (`eval/sets`) 极易混淆。

---

### 6.2 用量统计面板数据全部为 0

**症状**：管理后台用量统计页面显示总调用量/Token/对话数均为 0，但实际有对话发生

**排查步骤**：

1. **检查数据库是否有记录**：
   ```bash
   docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
     -c "SELECT COUNT(*), auth_type FROM chat_usage_logs GROUP BY auth_type;"
   ```
   - 如果没有记录 → `ChatStatAspect` 没有触发（见下一步）
   - 如果有记录但统计仍为 0 → 查询有问题（见步骤 3）

2. **检查 ChatStatAspect 是否触发**：
   ```bash
   docker compose logs server --tail 200 | grep "ChatStatAspect"
   ```
   - 应看到 `ChatStatAspect: authType=WIDGET` 或 `authType=API_KEY`
   - 如果看到 `authType=JWT` → 预览对话，不计入统计（正常）
   - 如果完全没有 → AOP 没有拦截到对话方法

3. **检查 JPQL/native query 兼容性**：
   ```bash
   docker compose logs server --tail 200 | grep "聚合结果转long失败"
   ```
   - 如果出现此错误 → Hibernate + PostgreSQL 返回类型不兼容
   - **对策**：聚合查询必须使用 native query，不能用 JPQL（见 8.2 节）

4. **检查 authType 过滤条件**：
   - `authType <> 'JWT'` 包含 API_KEY 和 WIDGET 两种鉴权类型
   - 如果旧代码用了 `authType = 'API_KEY'` 会漏掉 WIDGET 类型的记录

---

### 6.3 评测集接口报错 / 评测报系统繁忙

**症状**：点击评测集页面报错，或执行评测提示系统繁忙

**排查步骤**：

1. **确认前端 API 路径与后端对齐**（同 6.1）

2. **检查 JWT 鉴权是否生效**：
   ```bash
   # 不带 token 调用应返回 401
   curl -s http://localhost:8080/api/v1/eval/sets | head -1
   ```

3. **检查 Milvus 是否可用**：
   ```bash
   curl -s http://localhost:9091/healthz
   ```
   评测执行依赖 Milvus 向量检索，Milvus 不可用会导致评测失败

4. **检查 Redis 评测锁**：
   ```bash
   docker exec rag-saas-redis-1 redis-cli GET "docchat:eval:running:1"
   ```
   - 如果返回 "1" → 上一次评测异常退出，锁未释放
   - **手动释放**：`docker exec rag-saas-redis-1 redis-cli DEL "docchat:eval:running:1"`

---

### 6.4 聊天组件预览空白 / 无法交互

**症状**：管理后台组件配置页面的预览窗口空白，或组件渲染但无法点击输入

**排查步骤**：

1. **打开浏览器 DevTools**（F12），查看 Console 和 Network 面板

2. **检查 iframe 是否加载了 widget-preview.html**：
   - Network 面板应看到 `widget-preview.html` 请求（200）
   - 如果 404 → 后端静态资源未部署

3. **检查 widget.js 是否加载**：
   - Network 面板应看到 `widget.js` 请求（200）
   - Console 应有 `[DocChat] 开始初始化` 日志
   - 如果加载失败 → Nginx 代理或后端静态资源配置问题

4. **检查 widget token 是否有效**：
   - Console 应有 `[DocChat] 配置加载成功, enabled: true`
   - 如果 `配置加载失败或组件未启用` → token 无效或组件已禁用

5. **Edge 浏览器不显示组件**：
   - 确认不是浏览器扩展阻止了 iframe/脚本加载
   - 尝试在 VS Code 内置浏览器中测试（更宽松的安全策略）

---

### 6.5 成员账号看到管理员功能

**症状**：团队成员登录后仍能看到 LLM 配置等管理员菜单项

**排查步骤**：

1. **检查 localStorage 中的 role**：
   - 打开浏览器 DevTools → Application → Local Storage
   - 查看 `role` 值是否正确（ADMIN / MEMBER）

2. **检查是否切换账号后未刷新**：
   - 同一浏览器窗口切换账号时，localStorage 会被更新
   - 但 Vue 组件可能未重新渲染 → 强制刷新页面（F5）

3. **后端权限验证**：
   - 即使前端菜单显示了管理功能，后端 API 会返回 403
   - 可用成员 token 调用管理员 API 验证：
   ```bash
   curl -s http://localhost:8080/api/v1/llm-config \
     -H "Authorization: Bearer <member-jwt-token>"
   # 应返回 403
   ```

---

### 6.6 后端启动失败 / Bean 注册冲突

**症状**：后端容器启动后立即退出，日志中出现 Bean 重复注册

**排查步骤**：

1. **查看启动日志**：
   ```bash
   docker compose logs server --tail 100
   ```

2. **检查目录名与 Java 包名一致性**：
   - Java 包名用下划线（`module_tenant`），不能有连字符
   - 目录名与包名不一致会导致 Spring 扫描到两个相同 Bean

3. **检查 Flyway 迁移脚本**：
   ```bash
   docker exec rag-saas-postgres-1 psql -U docchat -d docchat \
     -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
   ```
   - 如果最新迁移 `success = false` → 修复迁移脚本后重试

---

## 7. 部署检查清单

每次部署前必须逐项确认：

### 7.1 构建产物

- [ ] `mvn package -DskipTests` 通过
- [ ] `pnpm build` 通过
- [ ] `npx vite build`（chat-widget）通过
- [ ] `widget.js` / `widget.css` 已复制到 `server/src/main/resources/static/`

### 7.2 环境变量

- [ ] `JWT_SECRET` 已设置（生产环境不能用默认值）
- [ ] `POSTGRES_PASSWORD` 已设置（生产环境不能用默认值）
- [ ] `DOCCHAT_LLM_API_URL` / `DOCCHAT_LLM_API_KEY` 已配置（否则 RAG 对话不可用）
- [ ] `SPRING_PROFILES_ACTIVE` 包含 `prod`（不加载 dev seed 数据）

### 7.3 服务验证

- [ ] `docker compose ps` 所有服务 healthy/running
- [ ] `curl http://localhost/api/v1/auth/login -X POST ...` 返回 JSON
- [ ] 浏览器访问 http://localhost 显示登录页
- [ ] 登录后各页面（知识库/组件/评测集/用量统计）可正常访问
- [ ] 聊天组件预览可交互

### 7.4 数据完整性

- [ ] Flyway 迁移全部成功
- [ ] 数据库中无空 tenant_id 的记录
- [ ] 向量集合（Milvus）与数据库知识库数据一致

---

## 8. 配置管理

### 8.1 环境变量优先级

Spring Boot 配置优先级（从高到低）：

1. Docker Compose `environment` 中的环境变量
2. `application-prod.yml` 中的配置
3. `application.yml` 中的默认值
4. `application-dev.yml` 中的开发环境值（仅 dev profile）

### 8.2 数据库查询：JPQL vs Native Query

**铁律：涉及日期/聚合的查询在 PostgreSQL 上必须使用 native query。**

| 场景 | JPQL | Native Query |
|------|------|-------------|
| 简单 CRUD | ✅ 推荐 | 不需要 |
| `CAST(expr AS type)` | ❌ 不可靠 | ✅ 使用 `DATE()` 等原生函数 |
| 多列聚合返回 `Object[]` | ❌ 可能嵌套 | ✅ 扁平返回 |
| H2 内存库测试 | ✅ 通过 | ✅ 通过 |
| PostgreSQL 生产 | ❌ 可能失败 | ✅ 可靠 |

**典型错误示例**：

```java
// ❌ JPQL：在 PostgreSQL 上可能返回嵌套 Object[]
@Query("SELECT COUNT(l), SUM(l.tokens) FROM Log l WHERE ...")
Object[] aggregate(...);

// ✅ Native Query：在 PostgreSQL 上可靠返回扁平数组
@Query(value = "SELECT COUNT(*), SUM(tokens) FROM logs WHERE ...", nativeQuery = true)
List<Object[]> aggregate(...);
```

**native query 返回值安全转换**：

```java
// PostgreSQL 可能返回 BigInteger/BigDecimal/Long/Integer
private long toLong(Object value) {
    if (value == null) return 0L;
    if (value instanceof Number num) return num.longValue();
    try { return Long.parseLong(value.toString()); }
    catch (Exception e) { return 0L; }
}
```

### 8.3 三种鉴权 Token 对照

| Token 类型 | 前缀 | 用途 | 计入用量 | 受每日限额 |
|-----------|------|------|---------|-----------|
| API Key | `dc_` | REST API 调用鉴权（程序化接入） | ✅ | ✅ |
| Widget Token | UUID | 嵌入式聊天组件鉴权（网站访客） | ✅ | ❌ |
| JWT Token | `eyJ` | 管理后台认证（含预览对话） | ❌ | ❌ |

**ChatController 自动识别规则**：
- `Bearer dc_xxx` → API Key 鉴权
- `Bearer eyJxxx` → JWT 鉴权（预览对话）
- `Bearer <其他>` → Widget Token 鉴权

### 8.4 Redis Key 规范

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `docchat:auth:login_fail:<email>` | 登录失败计数 | 30 分钟 |
| `docchat:eval:running:<setId>` | 评测执行锁 | 10 分钟 |
| `docchat:quota:<tenantId>:<date>` | API Key 每日调用计数 | 当日有效 |

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-28 | 初始版本：服务架构、启停重建、健康检查、日志、数据库、6 个常见故障排查、部署检查清单、配置管理 |
