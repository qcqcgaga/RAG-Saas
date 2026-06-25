# 单元测试报告

> 项目：DocChat — 文档智能客服 SaaS
> 日期：2026-06-24
> 执行时间：4.788 秒

## 1. 测试总览

| 指标 | 值 |
|------|-----|
| 测试类数 | 6 |
| 测试用例总数 | 73 |
| 通过 | 73 ✅ |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |

## 2. 各模块测试详情

| 测试类 | 用例数 | 通过 | 覆盖范围 |
|--------|--------|------|----------|
| JwtUtilTest | 12 | 12 ✅ | Token生成/解析/验证/过期/篡改/提取 |
| DocumentChunkerTest | 19 | 19 ✅ | 固定切分/重叠/合并/句子切分/段落切分/边界 |
| DocumentFileValidatorTest | 16 | 16 ✅ | 文件类型/大小/文件头/UUID命名/边界 |
| EmbeddingServiceTest | 6 | 6 ✅ | 维度/归一化/确定性/批量/空列表 |
| TaskServiceImplTest | 12 | 12 ✅ | 创建/查询/重试/进度/状态/跨租户 |
| AuthServiceImplTest | 8 | 8 ✅ | 注册/登录/锁定/禁用/失败计数/邮箱重复 |

## 3. 覆盖的核心业务逻辑

### 认证模块 (AuthServiceImpl)
- ✅ 正常注册流程（创建租户+用户+Token）
- ✅ 邮箱重复注册拒绝
- ✅ 特殊字符租户名 slug 生成
- ✅ 正常登录流程
- ✅ 邮箱不存在增加失败计数
- ✅ 密码错误增加失败计数
- ✅ 账户锁定（5次失败）
- ✅ 禁用账户拒绝登录（不增加失败计数）

### 任务模块 (TaskServiceImpl)
- ✅ 创建任务并推入队列
- ✅ 查询任务（正常/不存在/跨租户）
- ✅ 重试任务（正常/非FAILED/超最大次数）
- ✅ 进度值 0-100 范围限制
- ✅ 状态更新（PROCESSING设startedAt, COMPLETED设progress=100）

### 知识库模块
- ✅ 文档切分：固定大小/重叠/短块合并/句子/段落
- ✅ 文件校验：类型白名单/大小限制/PDF文件头/UUID命名
- ✅ Embedding：1536维/L2归一化/确定性输出/批量

### 安全模块 (JwtUtil)
- ✅ Token 生成与解析往返
- ✅ 有效/无效/篡改/过期/空Token验证
- ✅ userId/tenantId/role 提取

## 4. 未覆盖模块（待集成测试）

| 模块 | 原因 |
|------|------|
| KnowledgeServiceImpl | 复杂文件I/O，需 @TempDir + 多Repository mock |
| ChatServiceImpl | 虚拟线程 + SSE，需集成测试 |
| TaskQueueService | Redis交互，需嵌入式Redis或集成测试 |
| DocumentParser | PDFBox静态方法，需测试夹具文件 |
| WidgetServiceImpl | 简单CRUD，优先级较低 |
| TenantServiceImpl | MockedStatic<SecurityUtil>，优先级中等 |

## 5. 集成测试说明

DocChatApplicationTest（@SpringBootTest 上下文加载测试）需配合 TestContainers 运行，
当前标记为集成测试，在单元测试阶段排除执行。
