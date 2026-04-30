# SchemaPlexAI Claude Code 工作流 — 快速参考

> 一句话：在 `.claude/changes/<feat>/` 中按 `proposal → spec → design → tasks → apply → archive` 六阶段驱动变更。

---

## 目录结构

```
.claude/
├── CLAUDE-DEVELOPER.md          # v1 工作流指南（通用 agent 模式）
├── CLAUDE-DEVELOPER-v2.md       # v2 工作流指南（OpenSpec + Superpowers 融合）
├── changes/                     # 活跃变更沙盒（不提交 git）
│   ├── <feature-a>/
│   │   ├── proposal.md          # Phase 1: 提案
│   │   ├── spec.md              # Phase 2: 技术规格（v2 中为秘格）
│   │   ├── design.md            # Phase 3: 架构设计（可选）
│   │   ├── tasks.md             # Phase 4: Graphify 任务图
│   │   ├── context.md           # 任务上下文（Layer 4）
│   │   └── notes/               # 执行笔记、发现
│   └── archive/                 # 已完成变更归档
└── workflow/
    ├── README.md                # 本文件
    ├── templates/               # 变更模板
    │   ├── change-proposal.md
    │   ├── change-spec.md
    │   ├── change-design.md
    │   └── change-tasks.md
    └── scripts/                 # 辅助脚本
        ├── change-init.sh       # 初始化新变更
        ├── change-status.sh     # 查看活跃变更
        └── change-archive.sh    # 归档已完成变更
```

### 版本选择

| 场景 | 推荐版本 |
|------|---------|
| 已安装 Superpowers + OpenSpec | **v2** (`CLAUDE-DEVELOPER-v2.md`) |
| 仅使用通用 agent | v1 (`CLAUDE-DEVELOPER.md`) |
| 进行中 v1 变更 | 继续用 v1，无需迁移 |

---

## 常用操作

### 开始一个新 Feature

```bash
# 方式 1: 脚本
.claude/workflow/scripts/change-init.sh my-new-feature

# 方式 2: 直接对话
"我要实现 [功能描述]"
# Claude 会自动创建 .claude/changes/<feat>/ 目录并进入 Propose 阶段
```

### 查看当前进度

```bash
.claude/workflow/scripts/change-status.sh
```

### 完成并归档

```bash
.claude/workflow/scripts/change-archive.sh my-new-feature
```

---

## 各阶段速查

| 阶段 | 你做什么 | Claude 做什么 | 产出 |
|------|---------|--------------|------|
| **Propose** | 描述需求 | 对齐边界，写 proposal.md | `.claude/changes/<f>/proposal.md` |
| **Spec** | 确认/修改 | 写技术规格 | `.claude/changes/<f>/spec.md` |
| **Design** | 确认架构 | 画架构图，写 design.md | `.claude/changes/<f>/design.md` |
| **Plan** | 确认任务分解 | 生成 Graphify 任务图 | `.claude/changes/<f>/tasks.md` |
| **Apply** | 回答疑问 | 按任务图执行代码 | 代码 + 测试 |
| **Archive** | 确认归档 | 沉淀到 docs/ + wiki/ | 归档目录 + 更新文档 |

---

## 何时跳过阶段

| 场景 | 可跳过 | 必须保留 |
|------|--------|---------|
| < 50 行代码修改 | Propose, Design | tasks.md + Apply |
| Bug 修复（根因已知） | Propose | tasks.md + Apply + Archive |
| 纯前端 UI 调整 | Design | Spec（简化）+ tasks + Apply |
| > 200 行或跨模块 | — | 完整六阶段 |
| 新增服务/模块 | — | 完整六阶段 + ADR |

---

## 与 docs/ wiki/ 的交互

```
.claude/changes/<feat>/
    ├── spec.md  ──────┐
    ├── design.md ─────┼── 评审通过 ──→ docs/specs/<topic>.md
    └── tasks.md  ─────┘                docs/designs/<topic>.md
                                        docs/archive/ (旧版)

执行发现/决策 ──────────────────────────→ wiki/log.md
文档缺口 ───────────────────────────────→ wiki/gaps.md
```
