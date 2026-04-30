# 文档目录结构规范

## 统一结构

```
docs/
├── README.md                    # 文档总览和导航（已存在）
├── requirements/                # 产品需求文档（PRD）
│   └── README.md
├── specs/                       # 技术规格说明书
│   └── README.md
├── designs/                     # 架构设计文档
│   ├── README.md
│   └── YYYY-MM-DD-<topic>-design.md
├── plans/                       # 实施计划
│   ├── README.md
│   └── YYYY-MM-DD-<topic>-plan.md
├── decisions/                   # ADR（架构决策记录）
│   ├── README.md
│   ├── ADR-TEMPLATE.md
│   └── ADR-NNN-<title>.md
├── standards/                   # 开发规范与流程
│   ├── sdd-process.md           # SDD 流程
│   ├── tdd-guide.md             # TDD 指南
│   └── directory-structure.md   # 本文件
├── archive/                     # 已归档的旧版文档
│   ├── README.md
│   └── PROJECT_PLAN.md          # 示例：旧版计划
└── assets/                      # 图片等资源
    └── ...
```

## 文件命名规范

```
YYYY-MM-DD[-vX.Y]-<topic>.md
```

- `YYYY-MM-DD`：文档创建或重大修订日期
- `vX.Y`：版本号（可选，用于基线文档如架构设计）
- `topic`：短横线分隔的英文小写主题词

**正确示例**：
- `2026-04-29-v1.1-system-architecture.md`
- `2026-04-30-agent-runtime-task-board.md`
- `2026-05-01-user-auth-spec.md`

**错误示例**：
- `design.md`（无日期，无法排序）
- `新建文本文档.md`（无意义）
- `superpowers/plans/2026-04-30-schemaplexai-unified-development-plan.md`（目录层级违规）

## Plugin 输出约束

### 禁止

- 在 `docs/` 根目录直接创建文件
- 在 `docs/` 下创建 plugin 专属子目录（如 `docs/superpowers/`、`docs/ecc/`、`docs/ccg/`）
- 不经评审直接将 plugin 输出视为项目基线

### 正确做法

1. Plugin 生成的 plan/spec 先输出到临时位置：
   - 推荐：`.claude/outputs/`
   - 或 plugin 自己的缓存目录（如 `~/.claude/plugins/cache/`）
2. 人工/团队评审后，按本规范重命名并移入对应 `docs/` 子目录
3. 旧版 plugin 输出归档到 `docs/archive/`

## 现有文档迁移对照

| 原位置 | 新位置 | 说明 |
|--------|--------|------|
| `docs/PROJECT_PLAN.md` | `docs/archive/PROJECT_PLAN.md` | 旧版归档 |
| `docs/PROJECT_PLAN_REVISED.md` | `docs/plans/2026-04-29-v1.1-project-plan.md` | 修订版计划 |
| `docs/design/DESIGN_REVISED.md` | `docs/designs/2026-04-29-v1.1-system-architecture.md` | 架构设计 v1.1 |
| `docs/design/AGENT_RUNTIME_AND_TASK_BOARD_DESIGN.md` | `docs/designs/2026-04-30-agent-runtime-task-board.md` | Runtime 设计 |
| `docs/superpowers/plans/2026-04-30-...` | `docs/plans/2026-04-30-unified-dev-plan.md` | Plugin 计划（已评审） |
| `docs/*.jpg` | `docs/archive/assets/` | 图片资源归档 |
