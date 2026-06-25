# 部署记录

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0
> 部署日期：2026-06-25
> 操作人：开发团队

## 1. 部署环境

| 项目 | 值 |
|------|-----|
| 部署方式 | Docker Compose |
| 操作系统 | Windows 11 (Docker Desktop) |
| Docker版本 | 29.5.3 |
| Docker Compose | v2 |

## 2. 部署步骤执行记录

| 步骤 | 操作 | 结果 | 时间 |
|------|------|------|------|
| 1 | 后端JAR构建 `mvn clean package -DskipTests` | BUILD SUCCESS | 08:50:36 |
| 2 | 启动基础设施 `docker compose up -d postgres redis etcd minio milvus` | 全部 Running | 08:51 |
| 3 | 修复Dockerfile构建路径问题 | 修复完成 | 08:52 |
| 4 | 修复JWT_SECRET默认值过短 | 修复完成 | 09:03 |
| 5 | 修复logback prod profile不输出CONSOLE | 修复完成 | 09:04 |
| 6 | 修复Flyway seed迁移冲突 | 修复完成 | 09:05 |
| 7 | 启动后端服务 `docker compose up -d server` | Started | 09:12 |
| 8 | 前端本地构建 `pnpm build` | 构建成功 | 09:30 |
| 9 | 启动前端服务 `docker compose up -d web` | Started | 09:31 |

## 3. 部署中发现的问题及修复

### 问题1: Dockerfile context路径错误
- **现象**: `docker compose build server` 报错 `GetFileAttributesEx D:\agent_project\docker: The system cannot find the file specified`
- **根因**: docker-compose.yml 中 `context: ..` 指向项目根目录上级
- **修复**: `context: ..` → `context: .`，同时调整 Dockerfile 中 COPY 路径

### 问题2: JWT_SECRET默认值过短
- **现象**: Spring Boot 启动失败，`WeakKeyException: The specified key byte array is 176 bits`
- **根因**: `changeme-in-production` 只有 22 字节，不满足 HMAC-SHA256 >= 32 字节要求
- **修复**: docker-compose.yml 中 `JWT_SECRET: ${JWT_SECRET:-docchat-prod-jwt-secret-must-be-at-least-32-chars!!}`

### 问题3: logback prod profile不输出CONSOLE
- **现象**: `docker logs` 无 Spring Boot 日志
- **根因**: logback-spring.xml 中 prod profile 只配置了 FILE appender
- **修复**: prod profile 同时输出 CONSOLE + FILE

### 问题4: Flyway seed迁移冲突
- **现象**: `Validate failed: Migrations have failed validation`
- **根因**: dev profile 应用了 seed 迁移，prod profile 不包含
- **修复**: 删除 flyway_schema_history 中 seed 相关记录

### 问题5: 前端tsconfig.node.json配置错误
- **现象**: `vue-tsc -b` 报错 `allowImportingTsExtensions can only be used when either noEmit or emitDeclarationOnly is set`
- **根因**: tsconfig.node.json 中 `noEmit: false` 与 `allowImportingTsExtensions` 冲突
- **修复**: `noEmit: false` → `emitDeclarationOnly: true`

## 4. 服务状态

| 服务 | 状态 | 端口 | 健康检查 |
|------|------|------|---------|
| PostgreSQL | Running | 5432 | Healthy |
| Redis | Running | 6379 | Healthy |
| etcd | Running | 2379 | - |
| MinIO | Running | 9000-9001 | Healthy |
| Milvus | Running | 19530/9091 | Healthy |
| Spring Boot Server | Running | 8080 | OK |
| Nginx (Vue3) | Running | 80 | OK |

## 5. 验证结果

| 验证项 | 结果 |
|--------|------|
| 后端健康检查 (POST /api/v1/auth/login) | HTTP 500 -> 正常（需要请求体） |
| 注册接口 | code=0, 返回token |
| 登录接口 | code=0, 返回token |
| 获取租户信息 | code=0, 返回租户详情 |
| 获取知识库 | code=0, 返回默认知识库 |
| 前端访问 | HTTP 200 |

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-25 | MVP 0.1.0 部署完成，Docker Compose 7个服务全部启动 |
