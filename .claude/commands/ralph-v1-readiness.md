---
description: SchemaPlexAI v1 上线就绪 ralph-loop 主调度器；并行驱动 7 专家 + PlanModerator → 写 Δ 报告 → 检查终止条件
argument-hint: "[heavy|light|pr-<num>] (默认 heavy)"
---

# /ralph-v1-readiness · 主调度器

## 角色

ralph-loop 主控。每次重 loop（heavy）或 PR 触发轻 loop（light）均由你调度。**不写代码**，只编排子任务并合并产物。

## 输入

- 模式参数：`heavy`（每 24h 凌晨 03:17）/ `light`（PR 合入 main 触发）/ `pr-<num>`（指定 PR）
- 上次 loop：`.claude/plans/v1-readiness/loop-<最近日期>.md`
- 主报告：`docs/reviews/v1-readiness/MASTER.md`
- 评分基线：`docs/reviews/v1-readiness/scoring-matrix.md`
- 24h 内 commit：`git log --since="$(date -d '1 day ago' --iso-8601)" --oneline`
- 近期 hook 产物：`.claude/outputs/<today>/`（如有）

## 执行步骤

### Step 1：读取上轮基线

读 `.claude/plans/v1-readiness/loop-<最近日期>.md`，记下：
- 综合分
- 22 阻塞当前快照
- 5 跨域冲突状态
- 4 用户决策回执情况

### Step 2：判断模式

- **heavy**：执行 Step 3（全 7 专家）
- **light**：仅触发 `/expert-security` + `/expert-testdoc` + `/expert-debug`
- **pr-<num>**：light 模式 + 读 PR diff 限定 scope

### Step 3：并行调度 7 专家（heavy 模式）

**必须并行**——在同一条消息中通过 Agent 工具或 SendMessage 一次性派发：

1. `/expert-product` → ProductStrategist
2. `/expert-architecture` → ArchitectureAuditor
3. `/expert-dx` → DXArchitect
4. `/expert-design-system` → DesignSystemLead
5. `/expert-security` → SecurityAuditor（复用 `security-reviewer` agent）
6. `/expert-debug` → DebugMaster
7. `/expert-testdoc` → TestDocSentinel（复用 `code-reviewer` + `doc-gardener` agents）

每个专家覆盖自己的 `docs/reviews/v1-readiness/<domain>.md` 并加 changelog 头部行。

### Step 4：调度 PlanModerator

7 专家全部 return 后，调用 `/expert-moderator`：
- 合并 7 份新报告 → 重写 `MASTER.md`
- 头部 changelog 加新行：`<date> [Δ] 综合分 X.X → Y.Y, close N, 新增 M`
- 校验 5 跨域冲突仍成立 / 新出现冲突
- 更新 `scoring-matrix.md`（49 子维度）

### Step 5：写本轮 Δ 文件

新建 `.claude/plans/v1-readiness/loop-<YYYY-MM-DD>.md`，结构同 `loop-2026-05-08.md`：
1. 8 维度评分快照（带 Δ）
2. 阻塞清单（标 close / new / 仍开）
3. 跨域冲突状态
4. 用户决策回执
5. 下次 loop TODO
6. 终止条件检查

### Step 6：终止条件检查

| 条件 | 满足？ |
|------|-------|
| 7 专家全 ≥ 9/10 且连续 3 天无 Δ | 查最近 3 个 loop 文件 |
| v1 GA tag 已打 | `git tag -l "v1.0.0"` |
| 同一发现连续 5 轮未 close | 翻最近 5 loop 文件 |

任一满足即输出"**ralph-loop 已收敛**"摘要并终止；否则继续。

### Step 7：与 5 hooks 边界

**禁止重复跑**：lint / unit / build / format / commitlint（已由 PreToolUse / PostToolUse hook 覆盖）。

**专做**：评分 Δ、跨域冲突仲裁、覆盖率长趋势、文档 drift、新增阻塞。

Hook 失败时 → 直读 hook 产物（在 `.claude/outputs/` 或 stderr），不再触发。

## 输出

- 9 份覆盖：`docs/reviews/v1-readiness/<domain>.md`（7）+ `MASTER.md` + `scoring-matrix.md`
- 1 份新增：`.claude/plans/v1-readiness/loop-<YYYY-MM-DD>.md`
- 1 段摘要返回给用户：综合分 Δ + close/new 阻塞数 + 是否触发终止

## 关键原则

1. **并行**所有独立专家任务（Step 3 一次发出 7 个）
2. **不动业务代码** —— 落码归 `workflow-adopter` 8-phase
3. **不重做** —— 7 专家产物覆盖既有报告而非另起
4. **Δ 优先** —— 每行评分必须配 Δ；纯快照无价值
