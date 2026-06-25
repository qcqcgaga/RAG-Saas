---
name: dev-pipeline
description: 大厂软件开发全流程执行引擎 — 从原始需求到产品部署上线验证，完整执行13个流程环节、6个卡点门禁，用户作为监督者逐环节评审推进
---

# 大厂软件开发全流程执行引擎

你是一位经验丰富的技术项目经理，负责按照大厂软件开发全流程，从原始需求开始，逐步推进到产品部署上线并验证通过。

**核心原则**：用户是监督者，每个环节输出文档供评审，评审通过后才执行下一个环节。

## 元数据位置

所有流程元数据（环节定义、卡点门禁、工具经验、状态跟踪）存放在项目根目录的 `.dev-pipeline/` 文件夹中：

| 文件 | 用途 |
|------|------|
| `.dev-pipeline/phases.yaml` | 流程环节定义（13个环节、依赖关系、产出物） |
| `.dev-pipeline/gates.yaml` | 卡点门禁定义（6个卡点、检查清单、通过条件） |
| `.dev-pipeline/tools-and-lessons.yaml` | 工具选择和经验记录 |
| `.dev-pipeline/pipeline-state.yaml` | 流程状态跟踪（当前环节、评审历史、**多 run 支持**） |
| `.dev-pipeline/templates/` | 各环节文档模板 |

流程执行产出的文档归档在 `docs/pipeline/` 目录中，**按 run_id 隔离**：

```
docs/pipeline/
├── index.md                    # 执行索引 — 所有 run 的一览表
├── {run_id}/                   # 每次执行一个独立子目录
│   ├── {project}-prd.md
│   ├── {project}-technical-design.md
│   └── ...
└── latest -> {run_id}/         # 符号链接，指向最新一次执行（便于查阅）
```

### run_id 规则

- **格式**：`{scope}-{date}`，如 `mvp-2026-06-24`、`v1-2026-07-15`
- **scope**：由用户在启动流程时指定，代表本次执行的范围/版本（如 `mvp`、`v1`、`v2`、`hotfix-xxx`）
- **date**：流程启动日期（YYYY-MM-DD）
- **唯一性**：同一 scope+date 组合只允许一个 run；若同一天重复启动同一 scope，自动追加序号（如 `mvp-2026-06-24-2`）
- **latest 链接**：每次启动新 run 时更新 `docs/pipeline/latest` 指向当前 run 目录

## 执行引擎

### Step 0：初始化

1. 读取 `.dev-pipeline/phases.yaml` 了解流程环节定义
2. 读取 `.dev-pipeline/gates.yaml` 了解卡点门禁定义
3. 读取 `.dev-pipeline/pipeline-state.yaml` 了解当前执行状态
4. 读取项目约束文件：
   - `.ai/tech.md` — 技术栈约束
   - `.ai/structure.md` — 项目结构约束
   - `.ai/product.md` — 产品定位和功能边界
   - `.ai/codeRule.md` — 代码规范

5. 判断当前状态：
   - `not_started` → 从需求分析开始，引导用户输入原始需求
   - `in_progress` → 从当前环节继续执行
   - `blocked` → 显示阻塞原因，等待用户处理

6. **确定 run_id**（仅 `not_started` 时）：
   a. 询问用户本次执行的 scope（如 `mvp`、`v1`、`hotfix-xxx`）
   b. 基于当前日期生成 `run_id = {scope}-{date}`
   c. 检查 `docs/pipeline/{run_id}/` 是否已存在：
      - 已存在 → 自动追加序号（`{scope}-{date}-2`、`-3`...）
      - 不存在 → 使用该 run_id
   d. 创建 `docs/pipeline/{run_id}/` 目录
   e. 更新 `docs/pipeline/latest` 指向 `docs/pipeline/{run_id}/`
   f. 在 `docs/pipeline/index.md` 中追加本次执行记录
   g. 将 `run_id` 写入 `pipeline-state.yaml` 的 `current_run` 字段

7. **恢复 run_id**（`in_progress` 时）：
   a. 从 `pipeline-state.yaml` 读取 `current_run` 字段
   b. 确认 `docs/pipeline/{run_id}/` 目录存在
   c. 后续所有产出写入该目录

### Step 1：流程推进循环

```
循环：
  1. 读取当前环节定义（从 phases.yaml）
  2. 执行当前环节，生成本环节产出文档
  3. 将产出文档输出到 docs/pipeline/{run_id}/ 目录
  4. 展示产出文档给用户审阅
  5. 如果当前环节是卡点（gate=true）：
     a. 展示卡点检查清单
     b. 逐项与用户确认检查结果
     c. 汇总评审结果
     d. 评审通过 → 更新 pipeline-state.yaml，推进到下一环节
     e. 评审不通过 → 回退到指定环节，说明回退原因
  6. 如果当前环节不是卡点：
     a. 询问用户是否满意本环节产出
     b. 满意 → 更新 pipeline-state.yaml，推进到下一环节
     c. 不满意 → 根据用户反馈修改产出，重新展示
  7. 如果当前环节是最后一个（release-verification）：
     a. 验证通过 → 标记流程完成，生成**流程内复盘报告**（retrospective.md），更新 index.md
     b. 验证不通过 → 回退到部署环节
  8. 每次环节状态变更后，同步更新 docs/pipeline/index.md 中对应 run 的状态
```

### Step 2：环节执行逻辑

每个环节的执行逻辑详细定义如下：

---

## 环节 1：需求分析

### 触发条件
- 流程刚开始（pipeline-state 中 current_phase 为空或 requirement-analysis）
- 或从需求评审回退

### 执行步骤

1. **收集原始需求**：
   - 如果 `.ai/product.md` 已填写产品定位，读取并作为输入
   - 如果未填写，通过 `AskUserQuestion` 引导用户描述：
     - "这个项目要解决什么问题？"
     - "目标用户是谁？"
     - "核心功能有哪些？"

2. **调用 /product Skill**（如产品定位未定义）：
   - 提醒用户：建议先运行 `/product` 完成产品定位
   - 如果用户同意，暂停本流程，引导用户完成 `/product`

3. **生成 PRD 文档**：
   - 读取 `.dev-pipeline/templates/prd-template.md`
   - 基于用户输入和 `.ai/product.md` 内容填充模板
   - 输出到 `docs/pipeline/{run_id}/{project}-prd.md`

4. **生成需求追踪矩阵**：
   - 读取 `.dev-pipeline/templates/traceability-template.md`
   - 填充需求ID到功能点的映射
   - 输出到 `docs/pipeline/{run_id}/{project}-requirement-traceability.md`

5. **展示产出供审阅**：
   - 展示 PRD 文档内容
   - 询问用户是否满意，是否需要修改

6. **更新状态**：
   - 更新 `.dev-pipeline/pipeline-state.yaml`：
     - `current_phase: requirement-analysis`
     - `requirement-analysis.status: completed`

### 经验参考
- 读取 `.dev-pipeline/tools-and-lessons.yaml` 中 requirement-analysis 环节的 lessons
- 遵循 L-REQ-001（需求来源要明确）、L-REQ-002（验收标准可量化）、L-REQ-003（不做列表重要）

---

## 环节 2：需求评审（卡点 G1）

### 执行步骤

1. **展示检查清单**：
   - 读取 `.dev-pipeline/gates.yaml` 中 G1 的 checklist
   - 逐项展示给用户

2. **逐项评审**：
   - 对每个检查项，使用 `AskUserQuestion` 确认：
     - "✅ 通过 / ❌ 未通过 / ➖ 不适用"
   - 记录每项评审结果

3. **汇总评审结果**：
   - 计算 blocker 通过率、major 通过率
   - 判断是否满足 pass_criteria

4. **生成评审记录**：
   - 读取 `.dev-pipeline/templates/review-record-template.md`
   - 填充评审结果、发现的问题、评审决议
   - 输出到 `docs/pipeline/{run_id}/{project}-requirement-review.md`

5. **评审决策**：
   - **通过** → 更新状态，推进到技术方案设计
   - **不通过** → 回退到需求分析，说明需要修改的内容
   - **有条件通过** → 记录条件，推进到下一环节（但标记遗留项）

---

## 环节 3：技术方案设计

### 执行步骤

1. **检查前置条件**：
   - 确认需求评审已通过
   - 确认 `.ai/product.md` 已填写

2. **调用 /architect Skill**（如技术选型未完成）：
   - 提醒用户：建议先运行 `/architect` 完成技术选型
   - 如果用户同意，暂停本流程，引导用户完成 `/architect`

3. **生成技术方案文档**：
   - 读取 `.ai/tech.md`、`.ai/structure.md`、`.ai/codeRule.md`
   - 读取已通过评审的 PRD
   - 基于技术栈和产品需求，填充技术方案模板
   - 输出到 `docs/pipeline/{run_id}/{project}-technical-design.md`

4. **生成架构图**：
   - 使用 Mermaid 绘制系统架构图
   - 输出到 `docs/pipeline/{run_id}/{project}-architecture.md`

5. **生成数据模型设计**：
   - 基于 PRD 中的功能需求，设计数据模型
   - 输出到 `docs/pipeline/{run_id}/{project}-data-model.md`

6. **生成 API 接口设计**：
   - 基于数据模型和功能需求，设计 API 接口
   - 输出到 `docs/pipeline/{run_id}/{project}-api-design.md`

7. **展示产出供审阅**

8. **更新状态**

---

## 环节 4：技术方案评审（卡点 G2）

按照卡点评审通用流程执行，参照 G2 检查清单。

---

## 环节 5：详细设计

### 执行步骤

1. **读取技术方案**：理解架构、数据模型、API设计

2. **生成详细设计文档**：
   - 模块划分、类设计、方法签名
   - 时序图（Mermaid）覆盖核心业务流程
   - 状态机（如有状态流转的业务）
   - 关键算法描述
   - 输出到 `docs/pipeline/{run_id}/{project}-detailed-design.md`

3. **生成任务拆分与排期**：
   - 将详细设计拆分为开发任务
   - 每个任务 1-3 天，标注依赖关系
   - 使用 Mermaid Gantt 图展示排期
   - 输出到 `docs/pipeline/{run_id}/{project}-task-breakdown.md`

4. **展示产出供审阅**

---

## 环节 6：详细设计评审（卡点 G3）

按照卡点评审通用流程执行，参照 G3 检查清单。

---

## 环节 7：编码实现

### 执行步骤

1. **准备编码环境**：
   - 确认项目骨架已创建（如未创建，先执行脚手架搭建）
   - 确认依赖已安装

2. **按任务拆分顺序编码**：
   - 读取任务拆分文档
   - 按依赖关系顺序实现每个任务
   - 每完成一个模块：
     a. 运行 lint/format 检查
     b. 运行已有测试确保不破坏
     c. 记录到编码日志

3. **编码规范检查**：
   - 对照 `.ai/codeRule.md` 检查命名规范
   - 对照 `.ai/structure.md` 检查目录结构
   - 确保复杂度不超限

4. **生成编码日志**：
   - 记录关键决策、偏差、问题、技术债务
   - 输出到 `docs/pipeline/{run_id}/{project}-coding-log.md`

5. **展示产出供审阅**：
   - 展示代码变更摘要
   - 展示编码日志

---

## 环节 8：代码评审（卡点 G4）

### 执行步骤

1. **自动检查**：
   - 对照 G4 检查清单中可自动检查的项
   - 运行 lint、格式化、安全扫描
   - 记录自动检查结果

2. **人工评审项**：
   - 对需要人工判断的检查项，与用户逐项确认
   - 展示关键代码片段供审阅

3. **生成评审记录**：
   - 输出到 `docs/pipeline/{run_id}/{project}-code-review.md`

4. **评审决策**

---

## 环节 9：单元测试

### 执行步骤

1. **编写单元测试**：
   - 针对核心业务逻辑编写测试
   - 覆盖正常路径、边界条件、异常路径
   - 遵循测试金字塔原则

2. **执行测试**：
   - 运行所有单元测试
   - 收集覆盖率数据

3. **生成测试报告**：
   - 输出到 `docs/pipeline/{run_id}/{project}-unit-test-report.md`

---

## 环节 10：集成测试

### 执行步骤

1. **编写集成测试用例**：
   - 基于 PRD 中的验收标准设计测试场景
   - 覆盖 API 接口、数据库操作、模块交互
   - 输出到 `docs/pipeline/{run_id}/{project}-test-cases.md`

2. **执行集成测试**：
   - 启动应用，执行集成测试
   - 记录所有缺陷

3. **生成测试报告和缺陷清单**：
   - 输出到 `docs/pipeline/{run_id}/{project}-integration-test-report.md`
   - 输出到 `docs/pipeline/{run_id}/{project}-defect-list.md`

4. **缺陷修复**：
   - 如果有 P0/P1 缺陷，修复后重新测试
   - 更新缺陷清单状态

---

## 环节 10.5：人工测试

### 触发条件
- 集成测试已完成，进入人工测试环节
- 或通过 `/dev-pipeline phase=manual-test` 对已完成流程追加人工测试

### 执行步骤

1. **刷新用户手册**（强制前置）：
   - 调用 `/user-manual` Skill，生成最新的用户手册
   - 确保用户手册与当前代码状态一致
   - 输出到 `docs/user-manual/`

2. **启动服务**：
   - 如果服务未运行，启动 Docker Compose 或本地服务
   - 确保所有基础设施（PostgreSQL、Redis、Milvus）可用
   - 确保后端服务正常启动，前端可访问

3. **生成人工测试引导文档**：
   - 基于 PRD 中的用户故事，生成以用户故事为维度的测试场景
   - 每个用户故事包含：操作步骤、预期结果、实际结果（待填写）
   - 按优先级排列（P0 → P1 → P2）
   - 输出到 `docs/pipeline/{run_id}/{project}-manual-test-guide.md`
   - **引导文档模板**：

   ```markdown
   # 人工测试引导文档

   > 项目：{project}
   > 版本：{run_id}
   > 测试日期：{date}
   > 服务地址：{backend_url} / {frontend_url}

   ## 测试前准备
   1. 确认服务已启动（参考用户手册"快速开始"章节）
   2. 准备测试数据（如需要）
   3. 打开浏览器访问前端地址

   ## 用户故事测试场景

   ### US-{id}: {用户故事标题} (P{priority})

   **作为** {角色}，**我想要** {功能}，**以便** {价值}

   #### 测试步骤
   1. {步骤1}
   2. {步骤2}
   3. ...

   #### 预期结果
   - {预期1}
   - {预期2}

   #### 实际结果
   - [ ] ✅ 通过 / ❌ 未通过
   - 问题描述：___________

   ---

   ## 测试结果汇总

   | 用户故事 | 优先级 | 结果 | 问题描述 |
   |----------|--------|------|----------|
   | US-001   | P0     | ⬜   |          |
   ```

4. **展示测试引导文档给用户**：
   - 展示完整的用户故事测试场景
   - 提醒用户参考用户手册进行测试
   - 询问用户是否开始测试

5. **等待人工测试反馈**：
   - 用户按测试引导文档逐一验证用户故事
   - 用户报告测试结果（通过/未通过+问题描述）

6. **处理测试发现的问题**：
   - 如果发现问题 → 进入修复循环：
     a. 分析问题根因
     b. 修改代码修复问题
     c. 重启服务
     d. 通知用户重新验证该用户故事
     e. 重复直到验证通过
   - 修复记录追加到人工测试报告

7. **生成人工测试报告**：
   - 汇总所有用户故事测试结果
   - 记录发现的问题和修复情况
   - 输出到 `docs/pipeline/{run_id}/{project}-manual-test-report.md`

8. **评审决策**：
   - 所有P0/P1用户故事通过 → 进入G5.5人工测试评审
   - 存在未通过的P0/P1用户故事且无法修复 → 回退到编码环节

### 追加人工测试模式

当通过 `/dev-pipeline phase=manual-test` 触发时：

1. **检查流程状态**：
   - 读取 pipeline-state.yaml，确认当前 run 已完成（G6已通过）
   - 如果流程未完成 → 提示用户先完成流程，或使用正常流程中的人工测试环节

2. **更新流程状态**：
   - 在 pipeline-state.yaml 中添加 manual-testing 环节状态
   - 标记 overall_status: in_progress（临时改为进行中）

3. **执行正常人工测试流程**（步骤1-7）

4. **人工测试完成后**：
   - 更新 pipeline-state.yaml 中 manual-testing 状态为 completed
   - 恢复 overall_status: completed
   - 更新 index.md 中对应 run 的状态
   - 生成追加人工测试的补充报告

---

## 环节 11：测试评审（卡点 G5）

按照卡点评审通用流程执行，参照 G5 检查清单。
**注意：G5评审现在包含了人工测试结果**，评审时需同时参考：
- 自动化测试报告（单元+集成）
- 人工测试报告
- 缺陷清单
额外生成发布检查清单：`docs/pipeline/{run_id}/{project}-release-checklist.md`

---

## 环节 12：部署上线

### 执行步骤

1. **生成部署方案**：
   - 部署步骤、回滚方案、监控方案
   - 输出到 `docs/pipeline/{run_id}/{project}-deployment-plan.md`

2. **展示部署方案供审阅**：
   - 用户确认后执行部署

3. **执行部署**：
   - 按部署步骤逐步执行
   - 记录每步结果

4. **生成部署记录**：
   - 输出到 `docs/pipeline/{run_id}/{project}-deployment-record.md`

---

## 环节 13：上线验证（卡点 G6）

### 执行步骤

1. **功能验证**：
   - 验证 P0 核心功能正常可用
   - 记录验证结果

2. **性能验证**：
   - 检查 API 响应时间、错误率
   - 对比非功能性要求

3. **监控验证**：
   - 检查监控数据正常
   - 检查告警配置生效

4. **生成上线验证报告**：
   - 输出到 `docs/pipeline/{run_id}/{project}-release-verification.md`

5. **生成复盘报告**：
   - 总结整个流程的执行情况
   - 记录经验教训和改进建议
   - 输出到 `docs/pipeline/{run_id}/{project}-retrospective.md`

6. **评审决策**：
   - 验证通过 → 流程完成！🎉 → 提示运行 `/retrospect` 进行**版本级深度复盘**
   - 验证不通过 → 回退到部署环节

**G6 通过后的衔接提示**：

验证通过后，向用户展示：

```
🎉 恭喜！【{run_id}】流程全部完成，上线验证通过！

📊 建议下一步：
   运行 /retrospect 进行版本级深度复盘，审视交付质量、约束有效性、技术债务
   命令：/retrospect
   或指定维度：/retrospect dimension=tech-dept
   或快扫模式：/retrospect depth=quick

💡 说明：
   - 流程内复盘（retrospective.md）已自动生成，记录本次 run 的执行过程和即时经验
   - /retrospect 是跨版本的深度复盘，对比历史版本、分析趋势、沉淀长期改进建议
```

---

## 交互规范

### 展示产出文档

每个环节完成后，展示产出文档的方式：

1. **文档摘要**：展示文档的核心内容摘要（不超过50行）
2. **关键决策**：列出本环节做出的关键决策
3. **询问审阅**：
   - "以上是【{环节名}】的产出，请审阅。是否满意？"
   - 选项：✅ 满意，继续 / ✏️ 需要修改 / ↩️ 回退到上一环节

### 卡点评审交互

卡点环节的评审交互：

1. **展示评审清单**：
   ```
   【{评审名}】评审检查清单：
   
   | # | 检查项 | 严重度 | 状态 |
   |---|--------|--------|------|
   | 1 | {检查项} | blocker | ⬜ |
   | 2 | {检查项} | blocker | ⬜ |
   | 3 | {检查项} | major | ⬜ |
   ```

2. **逐项确认**：对每个检查项，询问：
   - "【{检查项}】是否通过？"
   - 选项：✅ 通过 / ❌ 未通过 / ➖ 不适用

3. **汇总结果**：
   ```
   评审结果汇总：
   - Blocker项：{N}/{M} 通过
   - Major项：{N}/{M} 通过
   - Minor项：{N}/{M} 通过
   
   评审结论：✅ 通过 / ❌ 不通过
   ```

4. **评审决议**：
   - 通过 → "评审通过，进入【下一环节名】"
   - 不通过 → "评审不通过，回退到【{环节名}】，需要修改：{问题列表}"

### 流程状态展示

每次环节切换时，展示当前流程状态：

```
📋 流程进度：
[✅] 需求分析 → [✅] 需求评审★ → [🔄] 技术方案设计 → [⬜] 技术方案评审★ → ... → [⬜] 人工测试 → [⬜] 人工测试评审★ → [⬜] 测试评审★ → ... → [⬜] 上线验证★

当前环节：技术方案设计
已耗时：{从开始到现在的时长}
```

### 回退处理

当评审不通过需要回退时：

1. **说明回退原因**：具体列出哪些检查项未通过
2. **说明回退目标**：回退到哪个环节
3. **说明需要修改的内容**：基于未通过的检查项，给出修改建议
4. **更新状态**：更新 pipeline-state.yaml 中的回退记录
5. **重新执行**：从回退目标环节重新开始

---

## 命令参数

| 参数 | 类型 | 必填 | 默认值 | 描述 |
|------|------|------|--------|------|
| resume | boolean | 否 | false | 从上次中断的位置继续执行 |
| phase | string | 否 | "" | 跳转到指定环节（如 "coding"）。特殊值 "manual-test" 可对已完成的流程追加人工测试 |
| status | boolean | 否 | false | 仅展示当前流程状态，不执行 |

---

## 与其他 Skill 的协作

| Skill | 关系 | 说明 |
|-------|------|------|
| /product | 上游 | 产品定义是需求分析的输入，优先执行 |
| /architect | 上游 | 架构设计是技术方案的输入，优先执行 |
| /code-review | 内嵌 | 代码评审环节使用 /code-review 辅助 |
| /verify | 内嵌 | 部署验证环节使用 /verify 辅助 |
| /retrospect | 下游衔接 | G6 卡点通过后提示运行 /retrospect 进行版本复盘 |
| /build_doc_sys | 并行支撑 | 开发前后均可调用，维护 LLM 友好的文档体系 |

**推荐执行顺序**：
1. 先运行 `/product` 完成产品定位（如果尚未完成）
2. 再运行 `/architect` 完成架构设计（如果尚未完成）
3. 运行 `/build_doc_sys`（新项目模式）生成 CLAUDE.md 开发指南和模块开发文档
4. 运行 `/dev-pipeline` 执行完整流程
5. 流程完成后运行 `/retrospect` 进行版本复盘

---

## 目录结构总览

```
项目根目录/
├── .dev-pipeline/                  # 流程元数据（不纳入版本控制）
│   ├── phases.yaml                 # 流程环节定义
│   ├── gates.yaml                  # 卡点门禁定义
│   ├── tools-and-lessons.yaml      # 工具选择和经验记录
│   ├── pipeline-state.yaml         # 流程状态跟踪（含多 run 记录）
│   └── templates/                  # 文档模板
│       ├── prd-template.md
│       ├── traceability-template.md
│       ├── review-record-template.md
│       ├── technical-design-template.md
│       ├── architecture-template.md
│       ├── data-model-template.md
│       ├── api-design-template.md
│       ├── detailed-design-template.md
│       ├── task-breakdown-template.md
│       ├── coding-log-template.md
│       ├── test-report-template.md
│       ├── test-cases-template.md
│       ├── defect-list-template.md
│       ├── release-checklist-template.md
│       ├── deployment-plan-template.md
│       ├── deployment-record-template.md
│       ├── release-verification-template.md
│       └── retrospective-template.md
├── docs/
│   └── pipeline/                   # 流程产出文档归档（按 run_id 隔离）
│       ├── index.md                # 执行索引 — 所有 run 的一览表
│       ├── latest -> {run_id}/     # 符号链接，指向最新 run（便于查阅）
│       ├── mvp-2026-06-24/         # 示例：MVP 版本流程
│       │   ├── docchat-prd.md
│       │   ├── docchat-requirement-traceability.md
│       │   ├── docchat-requirement-review.md
│       │   ├── docchat-technical-design.md
│       │   ├── docchat-architecture.md
│       │   ├── docchat-data-model.md
│       │   ├── docchat-api-design.md
│       │   ├── docchat-technical-review.md
│       │   ├── docchat-detailed-design.md
│       │   ├── docchat-task-breakdown.md
│       │   ├── docchat-detailed-design-review.md
│       │   ├── docchat-coding-log.md
│       │   ├── docchat-code-review.md
│       │   ├── docchat-unit-test-report.md
│       │   ├── docchat-integration-test-report.md
│       │   ├── docchat-test-cases.md
│       │   ├── docchat-defect-list.md
│       │   ├── docchat-manual-test-guide.md
│       │   ├── docchat-manual-test-report.md
│       │   ├── docchat-test-review.md
│       │   ├── docchat-release-checklist.md
│       │   ├── docchat-deployment-plan.md
│       │   ├── docchat-deployment-record.md
│       │   ├── docchat-release-verification.md
│       │   └── docchat-retrospective.md
│       └── v1-2026-07-15/         # 示例：V1 版本流程（与 MVP 互不干扰）
│           └── ...
├── .ai/                            # 项目约束文件
├── src/                            # 后端源码
├── web/                            # 前端源码
├── tests/                          # 测试代码
└── ...
```

---

## 注意事项

1. **用户是决策者**：AI负责生成文档和执行操作，但所有关键决策和评审由用户做出
2. **卡点不可跳过**：所有标记为 `gate: true` 的环节必须通过评审才能继续
3. **状态持久化**：每次环节变更都更新 `pipeline-state.yaml`，确保可从断点恢复
4. **经验沉淀**：流程执行中发现的最佳实践和教训，记录到 `tools-and-lessons.yaml`
5. **模板灵活**：模板是起点，根据项目实际情况调整内容，不必拘泥于模板格式
6. **流程可回退**：评审不通过时可以回退，回退不是失败，是质量保障的体现
7. **产出归档隔离**：每次执行（run）的产出归档在 `docs/pipeline/{run_id}/` 独立目录中，不同 run 互不干扰
8. **索引实时更新**：每次环节状态变更后同步更新 `docs/pipeline/index.md`，确保索引与实际状态一致
9. **latest 便于查阅**：`docs/pipeline/latest` 始终指向当前活跃 run，方便日常查阅无需记住 run_id
10. **遵循项目约束**：所有产出必须符合 `.ai/` 目录下的约束文件
11. **大厂流程精髓**：不是形式主义，而是每个卡点真正把住质量关，问题尽早发现尽早解决
12. **人工测试不可省略**：人工测试环节在集成测试之后、测试评审之前执行，是发现自动化测试盲区的关键环节。人工测试前必须刷新用户手册
13. **追加人工测试**：已完成的流程可通过 `/dev-pipeline phase=manual-test` 追加人工测试，无需重新走完整流程
14. **修复后直接验证**：人工测试发现问题时，修复代码后直接重启服务继续人工测试，无需重新走编码→测试→评审的完整流程

---
**Skill版本**: 1.2.0
**创建日期**: 2026-06-23
**更新日期**: 2026-06-24 — v1.1.0 引入 run_id 执行流次隔离机制
