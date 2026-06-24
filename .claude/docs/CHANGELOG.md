# 项目变更日志

> 记录关键变更、架构决策和风险提示

## 2026-06-24 — 文档体系构建

### 变更内容
- 构建完整的 LLM 友好文档体系（`.claude/docs/`）
- 重写 `CLAUDE.md` 为分层主索引（150行以内）
- 创建 9 个模块文档目录：common, module-tenant, module-knowledge, module-task, module-chat, module-widget, web-admin, chat-widget, deployment
- 每个模块包含：README.md + api.md + data-model.md + pitfalls.md
- 更新 `/build_doc_sys` Skill 定位为"贯穿全生命周期的支撑能力"
- 新增 `/retrospect` Skill（5维度复盘+流程衔接）
- 更新 `/dev-pipeline` Skill 增加与 `/retrospect` 的衔接提示
- 创建 `USAGE.md` 项目使用手册

### 风险评估
- **低风险**：文档变更不影响代码运行
- **注意**：CLAUDE.md 重写后，AI Agent 上下文中的项目认知已更新

### 回滚指南
- 如需回滚文档体系：删除 `.claude/docs/` 目录
- 如需回滚 CLAUDE.md：`git checkout HEAD -- CLAUDE.md`
- Skill 文件变更不影响代码，可直接 `git checkout HEAD -- .claude/commands/`

---

## 2026-06-23 — 项目骨架初始化

### 变更内容
- 完成产品定位（`.ai/product.md`）
- 完成技术栈选型（`.ai/tech.md`）
- 完成架构设计（`.ai/structure.md` + `.ai/codeRule.md`）
- 初始化项目骨架（5个MVP模块代码 + 前端 + chat-widget）
- Flyway 数据库迁移 V1-V4
- Docker 部署配置（Nginx + Server + PostgreSQL + Redis + Milvus）
- `/dev-pipeline` 流程产出（PRD + 技术方案 + 详细设计等 16份文档）

### 关键决策记录

| 决策 | 选择 | 权衡 |
|------|------|------|
| 后端语言 | Java 21 | 放弃 Python AI 生态便利 |
| 前端框架 | Vue 3 | 放弃 React 更大生态 |
| ORM | Spring Data JPA | 复杂查询控制力弱于 MyBatis |
| 向量数据库 | Milvus | 运维成本高于云托管 |
| 异步任务 | Redis + 自建队列 | 无 Dashboard，需自建监控 |
| LLM | 讯飞 Coding Plan API | 依赖服务可用性 |
