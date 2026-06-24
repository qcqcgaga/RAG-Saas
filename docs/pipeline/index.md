# 流程执行索引

> 本文件记录所有 dev-pipeline 执行的历史和状态，便于快速查阅。
> 每次新 run 启动或状态变更时自动更新。

## 状态图例

| 图标 | 含义 |
|------|------|
| 🔄 | 进行中 |
| ✅ | 已完成 |
| ⏸️ | 已暂停/阻塞 |
| ❌ | 已回退/取消 |

## 执行记录

| Run ID | Scope | 启动日期 | 当前环节 | 状态 | 产出目录 |
|--------|-------|----------|----------|------|----------|
| mvp-2026-06-24 | MVP | 2026-06-24 | 单元测试 | 🔄 进行中 | [mvp-2026-06-24/](mvp-2026-06-24/) |

## 最新执行

当前活跃的执行：**mvp-2026-06-24**（可通过 [latest/](latest/) 快速访问）

## 详细进度

### mvp-2026-06-24 — MVP

| # | 环节 | 状态 | 产出物 |
|---|------|------|--------|
| 1 | 需求分析 | ✅ 完成 | [PRD](mvp-2026-06-24/docchat-prd.md)、[追踪矩阵](mvp-2026-06-24/docchat-requirement-traceability.md) |
| 2 | 需求评审 ★ | ✅ 通过 | [评审记录](mvp-2026-06-24/docchat-requirement-review.md) |
| 3 | 技术方案设计 | ✅ 完成 | [技术方案](mvp-2026-06-24/docchat-technical-design.md)、[架构图](mvp-2026-06-24/docchat-architecture.md)、[数据模型](mvp-2026-06-24/docchat-data-model.md)、[API设计](mvp-2026-06-24/docchat-api-design.md) |
| 4 | 技术方案评审 ★ | ✅ 通过 | [评审记录](mvp-2026-06-24/docchat-technical-review.md) |
| 5 | 详细设计 | ✅ 完成 | [详细设计](mvp-2026-06-24/docchat-detailed-design.md)、[任务拆分](mvp-2026-06-24/docchat-task-breakdown.md) |
| 6 | 详细设计评审 ★ | ✅ 通过 | [评审记录](mvp-2026-06-24/docchat-detailed-design-review.md) |
| 7 | 编码实现 | ✅ 完成 | [编码日志](mvp-2026-06-24/docchat-coding-log.md) |
| 8 | 代码评审 ★ | ✅ 通过 | [评审记录](mvp-2026-06-24/docchat-code-review.md) |
| 9 | 单元测试 | 🔄 进行中 | — |
| 10 | 集成测试 | ⬜ 未开始 | — |
| 11 | 测试评审 ★ | ⬜ 未开始 | — |
| 12 | 部署上线 | ⬜ 未开始 | — |
| 13 | 上线验证 ★ | ⬜ 未开始 | — |
