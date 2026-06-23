# 体量项目

## 项目概述

DocChat — 面向独立开发者/小团队的文档智能客服 SaaS。用户上传产品文档/FAQ，生成可嵌入网站的聊天组件。

## AI Steering 约束

本项目在 `.ai/` 目录下维护项目约束文件，**所有代码生成和修改必须遵守这些约束**：

| 文件 | 用途 | 必须遵守 |
|------|------|----------|
| [.ai/tech.md](.ai/tech.md) | 技术栈约定（语言、框架、版本） | ✅ |
| [.ai/structure.md](.ai/structure.md) | 项目目录结构和分层架构规范 | ✅ |
| [.ai/product.md](.ai/product.md) | 产品定位和功能边界 | ✅ |
| [.ai/codeRule.md](.ai/codeRule.md) | 代码规范（命名、复杂度限制、安全规则） | ✅ |

### 约束优先级

1. `.ai/codeRule.md` — 代码层面的硬性规则，不可违反
2. `.ai/structure.md` — 架构层面的组织规则，新增文件必须遵循
3. `.ai/tech.md` — 技术选型约束，不得引入未列出的技术
4. `.ai/product.md` — 功能边界，不得实现"不做"列表中的功能

### 变更流程

- 约束文件变更需明确记录原因和日期
- 技术栈变更需同步更新 `tech.md`、`structure.md`、`codeRule.md`
- 功能增减需更新 `product.md`

## 当前状态

- [x] 确定产品功能（填写 `.ai/product.md`）
- [x] 确定技术栈（填写 `.ai/tech.md`）
- [x] 完成架构设计（填写 `.ai/structure.md` + `.ai/codeRule.md`）
- [x] 初始化项目骨架
- [ ] MVP：租户管理 + 知识库管理 + 异步任务处理 + RAG 对话服务 + 聊天组件嵌入
- [ ] V1：API Key 与访问控制 + 用量统计 + 评测集
