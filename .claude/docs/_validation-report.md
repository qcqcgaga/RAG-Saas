# 交叉验证报告

## 检查结果

### ✅ 文件大小合规

| 文件 | 行数 | 上限 | 状态 |
|------|------|------|------|
| CLAUDE.md | 77 | 150 | ✅ |
| common/README.md | 66 | 200 | ✅ |
| module-knowledge/README.md | 74 | 200 | ✅ |
| module-task/README.md | 71 | 200 | ✅ |
| module-chat/README.md | 60 | 200 | ✅ |
| module-widget/README.md | 54 | 200 | ✅ |
| web-admin/README.md | 65 | 200 | ✅ |
| chat-widget/README.md | 69 | 200 | ✅ |
| deployment/README.md | 72 | 200 | ✅ |

### ✅ 链接完整性

CLAUDE.md 中所有 `.claude/docs/` 链接均指向存在的文件。

### ✅ 模块覆盖完整性

| 代码模块 | 文档目录 | 状态 |
|----------|----------|------|
| common/ | docs/common/ | ✅ |
| module_tenant/ | docs/module-tenant/ | ✅ |
| module_knowledge/ | docs/module-knowledge/ | ✅ |
| module-task/ | docs/module-task/ | ✅ |
| module_chat/ | docs/module-chat/ | ✅ |
| module_widget/ | docs/module-widget/ | ✅ |
| web/ | docs/web-admin/ | ✅ |
| packages/chat-widget/ | docs/chat-widget/ | ✅ |
| docker/ | docs/deployment/ | ✅ |
| module-apikey/ (V1占位) | - | ⏭️ V1再补 |
| module-stat/ (V1占位) | - | ⏭️ V1再补 |
| module-eval/ (V1占位) | - | ⏭️ V1再补 |

### ⚠️ 发现的命名不一致

**问题**：代码中实际有代码的模块使用下划线命名（`module_tenant`、`module_knowledge`、`module_chat`、`module_widget`），而占位模块使用连字符（`module-apikey`、`module-stat`、`module-eval`、`module-task`、`module-tenant`、`module-chat`、`module-widget`）。

**根因**：`.ai/structure.md` 定义模块名为连字符（如 `module-tenant`），但 Java 包名不允许连字符。实际代码中部分模块创建时使用了下划线替代。

**影响**：文档统一使用连字符（如 `module-tenant`），与 `.ai/structure.md` 一致，但与实际 Java 包名（`module_tenant`）不一致。

**建议**：
- 短期：在 CLAUDE.md 中注明此命名差异
- 长期：统一规范——Java 包名用下划线（受语言限制），文档和配置用连字符（.ai/ 约束）

### ✅ 数据模型一致性

文档中描述的表结构（tenants, users, knowledge_bases, knowledge_documents, document_versions, async_tasks, widget_configs）与 Flyway 迁移脚本 V1-V4 一致。

### ✅ API 端点一致性

文档中记录的 API 端点与 Controller 代码一致：
- AuthController: /api/v1/auth (register, login) ✅
- TenantController: /api/v1/tenants (current, members) ✅
- KnowledgeController: /api/v1/knowledge (documents, versions) ✅
- TaskController: /api/v1/tasks (list, detail, retry) ✅
- ChatController: /api/v1/chat/conversations ✅
- WidgetController: /api/v1/widget (config, embed-script, regenerate-token) ✅
