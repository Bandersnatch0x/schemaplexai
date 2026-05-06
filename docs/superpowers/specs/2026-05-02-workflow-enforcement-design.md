---
topic: workflow-enforcement-closed-loop
stage: design
version: v1.0
status: 草稿
supersedes: ""
date: 2026-05-02
author: Claude (AI)
---

# 工作流强约束闭环设计

> **主题**: docs/wiki/ 三体系强约束联动 + CEK 模式集成
> **阶段**: design
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-05-02

---

## 1. 问题陈述

当前三套文档体系（`.claude/`、`docs/`、`wiki/`）职责已定义，但缺乏机械化的强制闭环：

- **docs/ ↔ wiki/ 同步靠人工**：wiki-constraints.md 定义了同步规则，但只在 Archive 阶段手动执行，导致开发过程中两套系统漂移
- **任务状态无 SSOT**：`.claude/changes/` 工作区是空 stub，`wiki/log.md` 只在归档后更新，没有"当前在做什么"的实时看板
- **质量门禁靠自觉**：workflow-adopter 的阶段转换没有自动化评分，依赖 Claude 自我约束

## 2. 设计目标

1. **docs/ 是唯一写入层**：所有人工/AI 写入都发生在 docs/，wiki/ 是只读派生视图
2. **wiki/ 自动生成**：脚本从 docs/ 状态 + git log 生成 wiki/ 内容，禁止手动编辑
3. **DEVELOPMENT_STATUS.md 自动生成**：Stop hook 从会话状态生成
4. **机械强制**：PreCommit hook + CI linter 替代"靠 Claude 自觉"
5. **LLM-as-Judge 门禁**：借鉴 CEK sdd 模式，阶段转换时自动评分

## 3. 整体架构

```
                    ┌──────────────────────────────────────┐
                    │         docs/ (SSOT 权威基线)         │
                    │  specs/ designs/ plans/ decisions/   │
                    │  standards/ requirements/             │
                    │  ← 人工/AI 写入，SDD 评审流程         │
                    └──────────┬───────────────┬───────────┘
                               │               │
              PreCommit hook   │               │  doc-gardening agent
              强制同步触发      │               │  定期扫描校验
                               │               │
                    ┌──────────▼───────────────▼───────────┐
                    │        wiki/ (派生视图，只读)          │
                    │  log.md gaps.md active-areas.md      │
                    │  entities/ controllers/ services/    │
                    │  ← 脚本自动生成，禁止手动编辑         │
                    └──────────┬───────────────────────────┘
                               │
                    Stop hook  │  每次会话结束
                               │
                    ┌──────────▼───────────────────────────┐
                    │  .claude/DEVELOPMENT_STATUS.md        │
                    │  ← 会话级任务看板（hook 自动生成）     │
                    │  格式：Week / Active / Done / Links   │
                    └──────────────────────────────────────┘
```

### 写入规则（Poka-Yoke）

| 体系 | 写入权限 | 约束机制 |
|------|---------|---------|
| `docs/` | 人工/AI 可写 | SDD 评审流程（不变） |
| `wiki/` | 只读（脚本生成） | PreCommit hook 阻止手动编辑 |
| `DEVELOPMENT_STATUS.md` | 只读（hook 生成） | PreCommit hook 阻止手动编辑 |

### 校验链

| 时机 | 校验内容 | 机制 |
|------|---------|------|
| PreCommit | docs/ 变更 → 必须调用 sync-wiki.sh | Hook 脚本 |
| Stop | 会话状态 → DEVELOPMENT_STATUS.md | Hook 脚本 |
| CI (定期) | docs/ vs 代码实际状态一致性 | doc-gardening agent |
| 阶段转换 | 任务质量评分 | LLM-as-Judge |

## 4. 组件设计

### 4.1 同步脚本 `scripts/sync-wiki.sh`

**职责**：从 docs/ 状态 + git log 自动生成 wiki/ 派生内容。

**生成规则**：

| 目标文件 | 数据源 | 生成逻辑 |
|---------|--------|---------|
| `wiki/log.md` | git log --oneline + docs/ YAML frontmatter | 提取最近 N 条 commit，关联 docs/ 状态变更 |
| `wiki/active-areas.md` | docs/specs/ + docs/plans/ | 筛选 status=进行中/评审中的条目 |
| `wiki/gaps.md` | 代码扫描 | 对比 entities/controllers/services 与 wiki/ 现有文档 |
| `wiki/decisions.md` | docs/decisions/ | ADR 索引，按状态分组 |
| `wiki/technical-debt.md` | docs/ + code TODO/FIXME | 聚合技术债标记 |

**标记规则**：所有自动生成的文件以 `<!-- AUTO-GENERATED: sync-wiki.sh at YYYY-MM-DDTHH:MM:SSZ -->` 开头。PreCommit hook 检测此标记的文件被手动修改时拒绝提交。

**调用方式**：
```bash
# 手动调用
./scripts/sync-wiki.sh

# PreCommit hook 自动调用
# (检测到 docs/ 变更时)
```

### 4.2 状态生成器 `scripts/gen-dev-status.sh`

**职责**：从当前会话状态生成 `.claude/DEVELOPMENT_STATUS.md`。

**数据源**：
- `.claude/changes/` 目录结构 → Active Changes 表
- `docs/*/` YAML frontmatter status 字段 → This Week 完成项
- `wiki/log.md` 最近条目 → Recent Decisions
- `docs/specs/` + `docs/plans/` → Links

**输出格式**：
```markdown
<!-- AUTO-GENERATED: gen-dev-status.sh at YYYY-MM-DDTHH:MM:SSZ -->
# Development Status — YYYY-MM-DD

## This Week
- [x] 已完成事项（从 docs/ status=已批准 提取）
- [ ] 进行中事项

## Active Changes
| Change | Phase | Last Updated |
|--------|-------|-------------|
| feat-name | Apply | YYYY-MM-DD |

## Recent Decisions
- ADR-NNN: 决策摘要

## Links
- [Specs](docs/specs/) | [Plans](docs/plans/) | [Wiki](wiki/)
```

### 4.3 PreCommit Hook

**文件**：`.claude/hooks/pre-commit-wiki-sync.sh`

**检测流程**：

```
1. git diff --cached --name-only → 变更文件列表
2. 如果包含 docs/ 下任何文件：
   a. 检查 .wiki-sync-stamp 文件时间戳
   b. 未同步 → 阻止提交，输出提示运行 ./scripts/sync-wiki.sh
3. 如果包含 wiki/ 下文件且无 AUTO-GENERATED 标记：
   a. 阻止提交，输出 "wiki/ 是只读派生视图，请修改 docs/ 后运行 sync-wiki.sh"
4. 如果包含 .claude/DEVELOPMENT_STATUS.md 且有手动编辑痕迹：
   a. 阻止提交，输出 "DEVELOPMENT_STATUS.md 由 hook 自动生成"
```

**配置方式**：在 `.claude/settings.local.json` 的 `hooks.PreToolUse` 中注册。

### 4.4 Stop Hook — 状态生成

**文件**：`.claude/hooks/stop-gen-status.sh`

**行为**：会话结束时调用 `gen-dev-status.sh`，更新 DEVELOPMENT_STATUS.md。

**配置方式**：在 `.claude/settings.local.json` 的 `hooks.Stop` 中注册。

### 4.5 CI 一致性校验脚本 `scripts/lint-docs-consistency.sh`

**职责**：在 CI 中校验 docs/ 与代码的一致性。

**校验项**：

| 检查 | 逻辑 | 严重度 |
|------|------|--------|
| Spec API 端点覆盖率 | 解析 docs/specs/ 中的 API 定义 vs Controller 代码 | HIGH |
| Entity 文档完整性 | 对比 entities/ 目录 vs wiki/entities/ | MEDIUM |
| Plan task 状态 | 检查 docs/plans/ 中 task 是否标记完成 | MEDIUM |
| YAML frontmatter 格式 | 验证所有 docs/ 文件的 frontmatter 字段 | HIGH |
| wiki/ AUTO-GENERATED 标记 | 确保所有 wiki/ 文件有自动生成标记 | HIGH |

### 4.6 Doc-Gardening Agent

**职责**：定期扫描 docs/ vs 代码实际状态，自动修复偏差。

**借鉴**：OpenAI Harness 的 doc-gardening 循环 + CEK Kaizen 持续改进。

**执行流程**：

```
1. 扫描 docs/ 中 status=已批准 的文档
2. 与代码对比：
   - spec 中 API 端点是否都在代码中实现
   - plan 中 task 是否都完成
   - entity 定义是否与数据库 schema 一致
3. 生成评分报告（借鉴 CEK Reflexion）：
   - 完整性评分（spec 覆盖率）
   - 新鲜度评分（最后更新时间）
   - 一致性评分（docs vs code 偏差数）
4. 对偏差项自动创建修复 PR
```

**触发方式**：
- CI cron job（每周）
- 手动调用：`/doc-gardening`

### 4.7 LLM-as-Judge 质量门禁

**集成位置**：改造 `.claude/skills/workflow-adopter/SKILL.md`

**借鉴**：CEK sdd/implement-task 的 LLM-as-Judge 验证模式。

**阶段门禁矩阵**：

| 阶段转换 | 最低评分 | 关键检查项 |
|---------|---------|-----------|
| Propose → Spec | 3.5 | 范围清晰、需求明确 |
| Spec → Design | 4.0 | 架构合理、边界清晰 |
| Design → Plan | 3.5 | 任务 ≤ 4h、有验收标准 |
| Plan → Apply | 3.5 | 每个 task 有验收标准、并行组识别 |
| Apply → Archive | 4.0 | 测试通过、Reflexion 评分完成 |

**评分维度**（从 CEK Reflexion 提取）：

| 维度 | 权重 | 说明 |
|------|------|------|
| 指令遵循 | 30% | 是否严格按 spec/plan 执行 |
| 输出完整性 | 25% | 所有需求是否覆盖 |
| 方案质量 | 25% | 代码质量、测试覆盖 |
| 推理质量 | 10% | 决策是否有理据 |
| 响应连贯性 | 10% | 文档/代码风格一致 |

## 5. 文件变更清单

### 新增文件

| 文件 | 用途 |
|------|------|
| `scripts/sync-wiki.sh` | docs/ → wiki/ 同步脚本 |
| `scripts/gen-dev-status.sh` | 生成 DEVELOPMENT_STATUS.md |
| `scripts/lint-docs-consistency.sh` | CI 一致性校验 |
| `.claude/hooks/pre-commit-wiki-sync.sh` | PreCommit hook |
| `.claude/hooks/stop-gen-status.sh` | Stop hook |
| `.claude/DEVELOPMENT_STATUS.md` | 会话级任务看板（hook 生成） |
| `docs/standards/2026-05-02-v1.0-doc-sync-rules.md` | docs/wiki/ 同步规范 |

### 修改文件

| 文件 | 变更内容 |
|------|---------|
| `.claude/settings.local.json` | 添加 PreToolUse/Stop hooks 配置 |
| `docs/standards/2026-04-30-v1.0-wiki-constraints.md` | wiki/ 改为只读自动生成 |
| `.claude/workflow/GUIDE.md` | 加入 LLM-as-Judge 门禁说明 |
| `.claude/skills/workflow-adopter/SKILL.md` | 集成 Judge 评分门禁 |
| `docs/standards/2026-04-30-v1.0-feature-workflow.md` | Archive 阶段加入 Reflexion 评分 |

## 6. 与 CEK 模式集成

| CEK 模式 | 集成方式 | 优先级 |
|---------|---------|--------|
| Poka-Yoke (防错) | PreCommit hook 阻止 wiki/ 手动编辑；CI linter 校验格式 | P0 |
| LLM-as-Judge | workflow-adopter 阶段转换时注入评分 | P0 |
| Reflexion | Archive 阶段产出加权评分报告 | P1 |
| Kaizen (持续改进) | doc-gardening agent 定期扫描 → 小步修复 PR | P1 |
| Subagent-Driven | 已有实现，补充任务间 review 评分记录 | P2 |
| minibeads 任务追踪 | 参考 frontmatter 格式设计 DEVELOPMENT_STATUS.md | P2 |

## 7. 实施顺序

1. **Phase 1 (P0)**：sync-wiki.sh + PreCommit hook + wiki/ 只读改造
2. **Phase 2 (P0)**：gen-dev-status.sh + Stop hook + DEVELOPMENT_STATUS.md
3. **Phase 3 (P1)**：LLM-as-Judge 集成到 workflow-adopter
4. **Phase 4 (P1)**：lint-docs-consistency.sh + CI 集成
5. **Phase 5 (P2)**：doc-gardening agent + Reflexion 评分报告

## 8. 验收标准

- [ ] 修改 docs/ 后 PreCommit hook 阻止提交直到 sync-wiki.sh 运行
- [ ] wiki/ 文件手动编辑被 PreCommit hook 拒绝
- [ ] Stop hook 自动生成 DEVELOPMENT_STATUS.md
- [ ] workflow-adopter 阶段转换时有 Judge 评分 ≥ 阈值
- [ ] CI 校验脚本检测到 docs/ vs 代码不一致时报错
- [ ] doc-gardening agent 能自动扫描并生成修复 PR
