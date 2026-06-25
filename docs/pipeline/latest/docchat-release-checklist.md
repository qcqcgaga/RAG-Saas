# 发布检查清单

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24
> 版本：MVP 0.1.0

## 部署前检查

### 代码质量

- [x] 所有 P0/P1 缺陷已修复并验证
- [x] 单元测试全部通过（73 用例）
- [x] 集成测试全部通过（53 用例）
- [x] 回归测试全部通过（127 用例）
- [x] 代码评审已通过（G4）

### 安全检查

- [x] 无硬编码密钥/连接串（生产配置使用环境变量）
- [x] JWT 密钥通过环境变量注入
- [x] 文件上传类型白名单 + 大小限制 + 文件头校验
- [x] 密码 BCrypt 哈希存储
- [x] 多租户数据隔离验证通过（8 项测试）
- [x] 统一异常处理不泄露内部信息
- [x] SQL 注入防护（JPA 参数化查询）

### 配置检查

- [x] 生产环境配置使用环境变量（application-prod.yml）
- [x] Flyway 数据库迁移脚本完整（V1-V4）
- [x] Docker Compose 配置完整（PG + Redis + Milvus + Server + Web）
- [x] Nginx 配置支持 SPA 路由 + API 反代 + SSE

### 构建检查

- [x] 后端编译通过（mvn compile）
- [x] 前端类型检查通过（vue-tsc --noEmit）
- [x] 聊天组件构建通过（vite build, IIFE 输出）
- [x] 应用可正常启动（Spring Boot 启动无报错）

## 部署步骤

1. 确认 Docker Desktop 运行中
2. 执行 `docker compose up -d` 启动基础设施（PostgreSQL + Redis + Milvus）
3. 等待基础设施就绪（约 30 秒）
4. 执行 `docker compose up -d server` 启动后端服务
5. 执行 `docker compose up -d web` 启动前端服务
6. 验证服务健康

## 部署后验证

- [ ] 后端健康检查：`curl http://localhost:8080/api/v1/auth/login` 返回非 500
- [ ] 前端可访问：`curl http://localhost:8081` 返回 200
- [ ] 注册功能正常
- [ ] 登录功能正常
- [ ] 知识库管理功能正常
- [ ] 文档上传功能正常
- [ ] 聊天组件嵌入脚本生成正常
- [ ] API 响应时间 P95 < 500ms（管理后台）
- [ ] API 响应时间 P95 < 2s（对话接口）

## 回滚方案

1. `docker compose down` 停止所有服务
2. `git checkout <上一个稳定版本>` 回退代码
3. 重新构建并部署

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-24 | 初始版本，MVP 发布检查清单 |
