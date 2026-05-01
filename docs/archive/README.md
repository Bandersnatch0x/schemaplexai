# Archive / 文档归档

已废弃或已被新版本取代的文档存放于此。归档文档仅供历史参考，不应作为当前决策依据。

## 归档规则

1. 活跃文档被新版本取代时，旧版复制到 `archive/`，文件名格式：
   ```
   YYYY-MM-DD-<topic>-<stage>-vX.Y.md
   ```
2. 归档文件保留原始内容（含旧版状态头部），不修改
3. 旧版活跃文档文件名（`<topic>.md`）由新版本接管
4. Plugin 原始输出未经评审的，先存放于此或 `.claude/outputs/`
5. `.claude/changes/` 执行空间完成后，完整副本归档于此

## 当前归档

### 被取代的活跃文档

| 文档 | 归档原因 |
|------|---------|
| [`PROJECT_PLAN.md`](PROJECT_PLAN.md) | 已被 `plans/project-plan.md` (v1.1) 取代 |
| [`2026-04-29-system-architecture-design-v1.1.md`](2026-04-29-system-architecture-design-v1.1.md) | 活跃文档已迁移至 `designs/system-architecture.md` |
| [`2026-04-30-agent-runtime-task-board-design-v1.0.md`](2026-04-30-agent-runtime-task-board-design-v1.0.md) | 活跃文档已迁移至 `designs/agent-runtime-task-board.md` |
| [`2026-04-29-project-plan-plan-v1.1.md`](2026-04-29-project-plan-plan-v1.1.md) | 活跃文档已迁移至 `plans/project-plan.md` |
| [`2026-04-30-unified-dev-plan-plan-v1.0.md`](2026-04-30-unified-dev-plan-plan-v1.0.md) | 活跃文档已迁移至 `plans/unified-dev-plan.md` |

### 执行空间归档

| 工作空间 | 内容 | 归档日期 |
|----------|------|----------|
| [`abyss-hive-ui/`](abyss-hive-ui/) | Abyss Hive UI 设计系统执行空间（spec/design/tasks/review） | 2026-05-02 |
| [`core-ai-engine-design/`](core-ai-engine-design/) | Core AI Engine Phase 2 设计评审空间 | 2026-05-02 |
| [`core-ai-engine-week1/`](core-ai-engine-week1/) | Core AI Engine Week 1 代码审查报告 | 2026-05-02 |
| [`wiki-gaps-completion/`](wiki-gaps-completion/) | Wiki 文档补全执行空间 | 2026-05-02 |
| [`roundtable-2026-05-01/`](roundtable-2026-05-01/) | Agentic Design Patterns 圆桌辩论输出 | 2026-05-02 |
| [`cursor-evaluation-first-2026-05-01/`](cursor-evaluation-first-2026-05-01/) | Cursor Evaluation-First 预研究输出 | 2026-05-01 |
| [`phase1-observability/`](phase1-observability/) | Phase 1 可观测性执行空间 | 2026-04-30 |
