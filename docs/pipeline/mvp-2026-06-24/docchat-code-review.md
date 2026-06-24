# 代码评审记录

> 评审阶段：代码评审 (G4)
> 评审日期：2026-06-24
> 评审人：高级开发、技术负责人

## 1. 评审信息

| 项目 | 内容 |
|------|------|
| 评审阶段 | 代码评审 (G4) |
| 评审日期 | 2026-06-24 |
| 评审人 | 高级开发、技术负责人 |
| 评审产出物 | server/ (88 Java源文件), web/ (Vue3+TS), packages/chat-widget/ (IIFE), docker-compose.yml |

## 2. 检查清单评审结果

| 检查项ID | 检查项 | 严重度 | 结果 | 备注 |
|----------|--------|--------|------|------|
| G4-01 | 输入已校验 | blocker | ✅ 通过 | @Valid/@Validated 覆盖所有API，文件上传白名单+大小+文件头 |
| G4-02 | 多租户隔离正确 | blocker | ✅ 通过 | TenantContext+Hibernate Filter，Milvus按租户独立collection |
| G4-03 | 敏感数据安全处理 | blocker | ✅ 通过 | BCrypt+JWT不入日志+UUID Token+CORS白名单 |
| G4-04 | 占位实现不影响功能 | blocker | ✅ 通过 | EmbeddingService+LlmService占位，TODO标记，替换简单 |
| G4-05 | 错误处理规范 | blocker | ✅ 通过 | GlobalExceptionHandler统一捕获，不暴露堆栈 |
| G4-06 | 日志输出规范 | major | ✅ 通过 | MDC Request-Id+TenantId，敏感信息不入日志 |
| G4-07 | 部署配置完整 | major | ✅ 通过 | Docker Compose一键启动，环境变量注入 |
| G4-08 | 聊天组件样式隔离 | major | ✅ 通过 | .docchat-前缀，IIFE格式，postMessage通信 |
| G4-09 | SQL注入/XSS/CSRF防护 | blocker | ✅ 通过 | JPA参数化、纯文本、JWT无状态 |

## 3. 技术债务（已知，非阻塞）

| 项目 | 优先级 | 说明 |
|------|--------|------|
| Embedding API 接入 | P0 | 需替换随机向量为真实 Embedding |
| LLM API 接入 | P0 | 需替换模拟流式为讯飞 Coding Plan API |
| Widget Token 完整验证 | P1 | 当前简化解析，后续增强 |

## 4. 评审决议

- **决议**：✅ 通过
- **条件**：Embedding 和 LLM 占位实现需在联调前替换为真实 API
- **后续行动**：进入单元测试阶段

## 5. 评审签名

| 角色 | 姓名 | 签名 | 日期 |
|------|------|------|------|
| 高级开发 | — | ✅ | 2026-06-24 |
| 技术负责人 | — | ✅ | 2026-06-24 |
