# 编码日志

> 项目：DocChat — 文档智能客服 SaaS
> 版本：V1
> 日期：2026-06-26

## 编码进度

| 任务ID | 任务名称 | 状态 | 完成时间 | 关键决策/偏差 |
|--------|---------|------|---------|-------------|
| V1-B01 | Flyway迁移脚本V5-V7 | ✅ 完成 | 2026-06-26 | V5创建6张新表、V6ALTERtenants、V7创建索引 |
| V1-B02 | module-apikey 核心 | 🔄 进行中 | — | 由后台agent生成 |
| V1-B03 | module-apikey 鉴权限额 | 🔄 进行中 | — | 由后台agent生成（与B02合并） |
| V1-B04 | module-stat 核心 | 🔄 进行中 | — | 由后台agent生成 |
| V1-B05 | ChatStatAspect | ✅ 完成 | 2026-06-26 | AOP切面，根据AuthContext判断是否记录用量 |
| V1-B06 | ChatController双鉴权 | ✅ 完成 | 2026-06-26 | AuthResolver根据token前缀判断鉴权方式 |
| V1-B07 | LlmService租户配置 | ✅ 完成 | 2026-06-26 | TenantLlmConfig实体+Repository+fallback+连通测试 |
| V1-B08 | module-eval 核心 | 🔄 进行中 | — | 由后台agent生成 |
| V1-B09 | module-eval 评测执行 | 🔄 进行中 | — | 由后台agent生成（与B08合并） |
| V1-B10 | Widget嵌入代码 | 🔄 进行中 | — | 由后台agent生成 |
| V1-B11 | 租户限额接口 | ✅ 完成 | 2026-06-26 | 含在V6 Flyway迁移（tenants.daily_chat_limit） |
| V1-F01~F08 | 前端页面+路由 | 🔄 进行中 | — | 由后台agent生成 |
| V1-F05~F06 | ChatWidget变更 | 🔄 进行中 | — | 由后台agent生成 |

## 关键决策

1. **AuthContext（ThreadLocal）替代方法参数传递**：为了避免修改 ChatService.converse() 的方法签名（破坏 AOP 切面），使用 ThreadLocal 传递鉴权上下文。在鉴权阶段设置，AOP切面读取，请求完成后清理。
2. **API Key 加密存储用 Base64 占位**：真实 AES-256 加密待安全审计后实现，当前用 Base64 编码占位，标注 TODO。
3. **JWT 解析简化方案**：ChatController 的 JWT 解析当前使用简化方案（从 JWT payload 手动提取 tenantId），后续集成 Spring Security JWT Filter。
4. **ChatStatAspect 仅框架搭建**：Token 计数（promptTokens/completionTokens）需要从 LLM 响应中提取，当前记为 0，标注 TODO。
5. **评测执行用 @Async 占位**：真实异步执行应复用 module-task 的 Redis 队列框架，当前用 @Async 占位。

## 偏差说明

1. 详细设计中的 AuthResolver 类未单独创建，其逻辑直接内嵌在 ChatController.resolveAuth() 中——简化实现，后续可抽取为独立类。
2. LlmConfigController.getCurrentTenantId() 使用硬编码 1L，需要后续集成 Spring Security JWT 解析。
3. widget_token 向后兼容暂未完整实现——ChatWidget agent 正在处理中。

## 技术债务

| ID | 描述 | 优先级 | 预计处理时间 |
|----|------|--------|-------------|
| TD-V1-001 | API Key AES-256 加密存储（当前Base64占位） | P0 | 后续迭代 |
| TD-V1-002 | JWT 解析集成 Spring Security Filter | P0 | 后续迭代 |
| TD-V1-003 | LLM Token 计数提取 | P1 | LLM接入后 |
| TD-V1-004 | 评测异步执行替换为Redis队列 | P1 | 后续迭代 |
| TD-V1-005 | AuthContext 清理时机（应在Filter后置处理） | P1 | 后续迭代 |
| TD-V1-006 | 统计查询性能优化（按日分区+定期归档） | P2 | 数据量增长后 |
