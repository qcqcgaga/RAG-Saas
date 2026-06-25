# 项目复盘报告

> 项目：DocChat — 文档智能客服 SaaS
> 版本：MVP 0.1.0
> 日期：2026-06-25

## 1. 流程执行总览

| 环节 | 状态 | 耗时 | 产出物 |
|------|------|------|--------|
| 需求分析 | 完成 | 1天 | PRD + 需求追踪矩阵 |
| 需求评审(G1) | 通过 | - | 3/3 blocker, 2/2 major |
| 技术方案设计 | 完成 | 1天 | 技术方案 + 架构图 + 数据模型 + API设计 |
| 技术方案评审(G2) | 通过 | - | 5/5 blocker, 2/2 major |
| 详细设计 | 完成 | 1天 | 详细设计 + 任务拆分 |
| 详细设计评审(G3) | 通过 | - | 2/2 blocker, 2/2 major |
| 编码实现 | 完成 | 1天 | 88 Java源文件 + Vue3前端 + IIFE组件 |
| 代码评审(G4) | 通过 | - | 8/8 blocker, 3/3 major |
| 单元测试 | 完成 | - | 6类73用例全通过 |
| 集成测试 | 完成 | - | 5类53用例全通过 |
| 测试评审(G5) | 通过 | - | 6/6 blocker, 1/1 major有条件 |
| 部署上线 | 完成 | - | Docker Compose 7个服务 |
| 上线验证(G6) | 通过 | - | 5/5 blocker |

**总耗时**: 1天（2026-06-24 至 2026-06-25）

## 2. 质量指标

| 指标 | 目标 | 实际 | 达成 |
|------|------|------|------|
| 单元测试通过率 | 100% | 100% | 是 |
| 集成测试通过率 | 100% | 100% | 是 |
| P0缺陷修复率 | 100% | 100% | 是 |
| P1缺陷修复率 | 100% | 100% | 是 |
| 代码评审通过率 | 100% | 100% | 是 |
| 卡点通过率 | 6/6 | 6/6 | 是 |

## 3. 关键经验教训

### L-TEST-004: H2替代TestContainers
- **场景**: Docker Desktop在Windows上TestContainers连接不稳定
- **做法**: 改用H2内存数据库+MockBean模拟外部依赖
- **效果**: 集成测试稳定运行，53用例全通过
- **建议**: Windows环境优先使用H2，Linux/macOS可用TestContainers

### L-DEPLOY-006: Dockerfile路径与docker-compose.yml一致
- **场景**: docker-compose.yml中context: ..导致构建失败
- **做法**: 统一使用context: .，调整Dockerfile中COPY路径
- **建议**: Dockerfile和docker-compose.yml放在同一目录，context用.

### L-DEPLOY-007: JWT_SECRET长度必须>=32字节
- **场景**: 默认值"changeme-in-production"只有22字节，HMAC-SHA256要求>=32字节
- **做法**: 使用64字节长的默认值
- **建议**: 生产环境必须配置环境变量，默认值仅用于开发

### L-DEPLOY-008: logback prod profile必须输出CONSOLE
- **场景**: Docker容器中prod profile只输出FILE，docker logs看不到日志
- **做法**: prod profile同时配置CONSOLE + FILE
- **建议**: 容器环境必须配置CONSOLE输出

## 4. 技术债务

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 性能测试 | P1 | G5遗留项，待压测验证 |
| Embedding替换 | P0 | 需接入真实Embedding API |
| LLM流式调用 | P0 | 需接入讯飞Coding Plan API |
| Widget Token完整验证 | P1 | 当前简化从Header解析tenantId |
| PDF大文件内存 | P2 | 大PDF解析可能OOM |
| 前端E2E测试 | P2 | Vitest单元测试待补充 |

## 5. 改进建议

| 建议 | 优先级 | 原因 |
|------|--------|------|
| CI/CD流水线 | P1 | 当前手动部署，需自动化 |
| 健康检查端点 | P1 | Spring Boot Actuator未启用 |
| 日志聚合 | P2 | 多容器日志分散，需ELK/Loki |
| 性能监控 | P2 | 无APM工具，需Micrometer+Prometheus |
| 告警配置 | P2 | 无告警机制 |

## 6. 流程改进建议

| 建议 | 说明 |
|------|------|
| G4增加Dockerfile检查项 | Dockerfile路径、JWT_SECRET长度等 |
| G5增加构建验证 | 前端build、Docker image build |
| 部署环节增加预检 | 端口占用检查、磁盘空间检查 |

## 7. 团队反馈

- 流程清晰，卡点机制有效保障了质量
- 自动化集成测试大幅提升了回归效率
- Docker部署环节问题较多，需要更多预检

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-06-25 | MVP 0.1.0 流程全部完成，生成复盘报告 |
