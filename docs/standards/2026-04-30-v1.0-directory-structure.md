# 文档目录结构规范

> **原则**：每个主题在每个阶段只有一份**活跃文档**，按主题命名。变更时旧版归档，新版替换。

---

## 统一结构

```
docs/
├── README.md                    # 文档总览和导航
├── requirements/                # 产品需求文档（PRD）
│   ├── README.md
│   └── <topic>.md               # 活跃需求文档（按主题命名）
├── specs/                       # 技术规格说明书
│   ├── README.md
│   └── <topic>.md               # 活跃规格文档
├── designs/                     # 架构设计文档
│   ├── README.md
│   └── <topic>.md               # 活跃设计文档
├── plans/                       # 实施计划
│   ├── README.md
│   └── <topic>.md               # 活跃计划文档
├── ui/                          # UI/UX 设计文档
│   ├── README.md
│   └── <topic>.md               # 活跃 UI 文档
├── decisions/                   # ADR（架构决策记录）
│   ├── README.md
│   ├── ADR-TEMPLATE.md
│   └── ADR-NNN-<title>.md
├── standards/                   # 开发规范与流程
│   ├── sdd-process.md
│   ├── tdd-guide.md
│   ├── directory-structure.md   # 本文件
│   └── DOCUMENT-TEMPLATE.md     # 活跃文档标准模板
├── archive/                     # 已归档的旧版文档
│   ├── README.md
│   └── YYYY-MM-DD-<topic>-<stage>-vX.Y.md
└── assets/                      # 图片等资源
    └── ...
```

## 文件命名规范

### 活跃文档（Active）

```
<topic>.md
```

- `topic`：短横线分隔的英文小写主题词，贯穿所有阶段保持一致
- **同一主题在不同阶段使用完全相同的 `topic` 名**

**示例**：
- `docs/designs/agent-runtime-task-board.md`
- `docs/specs/agent-runtime-task-board.md`
- `docs/plans/agent-runtime-task-board.md`
- `docs/ui/agent-runtime-task-board.md`

### 归档文档（Archived）

当活跃文档被新版本取代时，旧版按以下格式移入 `archive/`：

```
YYYY-MM-DD-<topic>-<stage>-vX.Y.md
```

- `YYYY-MM-DD`：本次版本创建或重大修订日期
- `topic`：与活跃文档一致的主题词
- `stage`：`requirement` / `spec` / `design` / `plan` / `ui`
- `vX.Y`：版本号

**示例**：
- `docs/archive/2026-04-30-agent-runtime-task-board-design-v1.0.md`
- `docs/archive/2026-05-01-agent-runtime-task-board-design-v1.1.md`

## 文档状态头部

每份活跃文档**必须**在开头使用 YAML front-matter 标注状态：

```markdown
---
topic: agent-runtime-task-board
stage: design
version: v1.0
status: 草稿
supersedes: ""                # 被本文档取代的前一版本归档路径（首次创建留空）
---
```

### 状态值定义

| 状态 | 说明 |
|------|------|
| **草稿** | 编写中，尚未评审 |
| **评审中** | 已提交评审，等待反馈 |
| **已批准** | 评审通过，可作为编码依据 |
| **已作废** | 被新版本取代，不再作为依据 |

### 版本演进示例

```
# v1.0 创建
活跃: docs/designs/agent-runtime-task-board.md  (status: 草稿)

# v1.0 评审通过
活跃: docs/designs/agent-runtime-task-board.md  (status: 已批准)

# v1.1 修订
归档: docs/archive/2026-04-30-agent-runtime-task-board-design-v1.0.md
活跃: docs/designs/agent-runtime-task-board.md  (status: 已批准, version: v1.1, supersedes: 2026-04-30-agent-runtime-task-board-design-v1.0.md)
```

## 文档变更流程

1. **创建**：按模板新建活跃文档，`status: 草稿`
2. **评审**：修订内容，`status: 评审中` → 收集反馈 → `status: 已批准`
3. **变更**：需要修订时，将当前活跃文档复制到 `archive/` 并补充日期/版本/阶段后缀，然后修改活跃文档内容，更新 `version` 和 `supersedes`
4. **作废**：若主题废弃，将活跃文档移入 `archive/`，状态改为 `已作废`，不保留活跃文档

## Plugin 输出约束

### 禁止

- 在 `docs/` 根目录直接创建文件
- 在 `docs/` 下创建 plugin 专属子目录（如 `docs/superpowers/`、`docs/ecc/`）
- 不经评审直接将 plugin 输出视为项目基线

### 正确做法

1. Plugin 生成的 plan/spec 先输出到临时位置：
   - 推荐：`.claude/outputs/`
   - 或 plugin 自己的缓存目录
2. 人工/团队评审后，按本规范创建/更新活跃文档
3. 若替换了旧版活跃文档，旧版按规范归档到 `docs/archive/`
