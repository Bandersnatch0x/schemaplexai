---
name: workflow-adopter
description: Autonomous 8-phase workflow loop for SchemaPlexAI changes. Runs Propose → Review → Spec → Design → Plan → Apply → Deliver → Archive with roundtable expert review, quality gates, TDD, and persistent state tracking until all phases pass. Use when the user says "workflow-loop", "full workflow", "auto-build", or "run the change end-to-end".
argument-hint: "<feature-name> <description>"
---

# Workflow Adopter

Autonomous 8-phase change lifecycle that persists across iterations until all quality gates pass. Includes roundtable expert review (4 perspectives) and verification delivery. Inspired by the Ralph loop pattern — loops on failure, gates on quality, completes on evidence.

## Purpose

Turn a feature description into a fully implemented, tested, reviewed, and archived change. Each phase has quality gates. If a gate fails, the loop re-runs the current phase with the failure context. No phase advances without passing its gate.

## When to Use

- User says "workflow-loop", "full workflow", "auto-build", "run the change end-to-end"
- User provides a feature description and wants the full lifecycle automated
- Multi-file changes that benefit from structured phase progression

## When NOT to Use

- Simple single-file fixes (< 50 lines) — just fix it directly
- User wants manual control over each phase — use the individual commands instead
- Exploratory research — use brainstorming skill first

## Arguments

Parse `$ARGUMENTS` as: `<feature-name> <description>`

- `feature-name`: Kebab-case identifier (e.g., `agent-pause-resume`)
- `description`: One-line feature description

If no arguments, check `.claude/workflow-loop.local.md` for active loop state.

## State File

Location: `.claude/workflow-loop.local.md`

```yaml
---
active: true
feature_name: <name>
description: <desc>
current_phase: propose|review|spec|design|plan|apply|deliver|archive
iteration: 1
max_iterations: 20
started_at: <ISO timestamp>
phases:
  propose:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
  review:
    status: pending|in_progress|passed|failed|rejected
    gate_result: null|pass|fail
    notes: ""
    verdict: null|approved|modified|rejected
  spec:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
  design:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
  plan:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
  apply:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
    files_changed: []
    tests_passing: false
  deliver:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
  archive:
    status: pending|in_progress|passed|failed
    gate_result: null|pass|fail
    notes: ""
skip_rules:
  lines_estimate: 0
  cross_module: false
  new_service: false
---
```

## Steps

### Step 0: Initialize (first invocation only)

1. Parse `$ARGUMENTS` for feature name and description
2. Create state file at `.claude/workflow-loop.local.md` with initial state
3. Apply skip rules from context:

| Scenario | Skip Phases | Must Keep |
|----------|-------------|-----------|
| < 50 lines | Propose, Review, Spec, Design | Plan + Apply + Deliver + Archive |
| Bug fix (root cause known) | Propose, Review | Spec + Plan + Apply + Deliver + Archive |
| Pure frontend UI adjustment | Design | Spec (simplified) + Plan + Apply + Deliver |
| > 200 lines or cross-module | — | All 8 phases |
| New service/module | — | All 8 phases + ADR |

4. Update `phases.*.status` to `skipped` for skipped phases
5. Report initialization summary

### Step 1: Read State

1. Read `.claude/workflow-loop.local.md`
2. Determine `current_phase` — the first phase with `status: pending` or `status: failed`
3. Check `iteration` against `max_iterations`. If exceeded, report and stop.
4. Increment `iteration`

### Step 2: Execute Current Phase

#### Phase: Propose

**Action**: Create/update `.claude/changes/<feature-name>/proposal.md` using the template at `.claude/workflow/templates/change-proposal.md`.

Content must include:
- One-line description from user input
- Background & motivation (research the problem in existing code)
- Measurable success criteria
- In/Out scope boundaries
- Impact assessment (which modules are affected)
- Risk assessment

**Quality Gate**: `proposal-gate`
- [ ] proposal.md exists and is non-empty
- [ ] Problem statement is specific and verifiable
- [ ] Scope boundaries are explicit (In/Out)
- [ ] At least one impact module identified
- [ ] No TBD/TODO placeholders remain

**On fail**: Re-run with gate failure notes injected as context.

#### Phase: Review (Roundtable)

**Action**: 4 位专家独立并行审查 proposal.md，综合意见形成裁决。

**Roundtable process**:

1. **Spawn 4 expert agents in parallel**, each reading `.claude/changes/<feature-name>/proposal.md` + `context.md`:

```
Task(subagent_type="general-purpose", name="expert-product",
  prompt="你是产品视角专家。审查以下 proposal.md，从用户价值、业务优先级、ROI、
  范围合理性角度给出评审意见。输出格式：
  - Verdict: approved/modified/rejected
  - Critical issues: (列表)
  - Suggestions: (列表)
  - Scope adjustments: (如有)
  [proposal.md 内容]")

Task(subagent_type="general-purpose", name="expert-architect",
  prompt="你是架构视角专家。审查以下 proposal.md，从模块边界、依赖关系、
  可扩展性、技术债务角度给出评审意见。输出格式同上。
  参考 CLAUDE.md 中的服务地图和模块链。
  [proposal.md 内容]")

Task(subagent_type="general-purpose", name="expert-security",
  prompt="你是安全视角专家。审查以下 proposal.md，从 OWASP Top 10、
  数据隔离、认证授权、输入验证、租户安全角度给出评审意见。输出格式同上。
  [proposal.md 内容]")

Task(subagent_type="general-purpose", name="expert-ai-engineer",
  prompt="你是 AI 工程视角专家。审查以下 proposal.md，从 LLM 集成、
  Token 管理、状态机设计、可观测性、性能角度给出评审意见。输出格式同上。
  参考 wiki/architecture-gap-analysis.md 中的模式差距。
  [proposal.md 内容]")
```

2. **Synthesize**: 收集 4 位专家意见，生成共识矩阵：

| 维度 | 产品 | 架构 | 安全 | AI工程 | 共识 |
|------|------|------|------|--------|------|
| 范围合理性 | | | | | |
| 技术可行性 | | | | | |
| 安全合规 | | | | | |
| 架构一致性 | | | | | |

3. **Verdict rules**:
   - **Approved**: 4/4 或 3/4 同意，无 Critical issue
   - **Modified**: 有修改建议但无阻断问题 → 更新 proposal.md 后继续
   - **Rejected**: 2+ 专家给出 Critical issue → 退回 Propose 阶段

4. **Write review report**: `.claude/changes/<feature-name>/review-report.md`

**Quality Gate**: `review-gate`
- [ ] 4 位专家意见均已收集
- [ ] 共识矩阵已生成
- [ ] Verdict 为 approved 或 modified
- [ ] 所有 Critical issue 已解决或标记为接受风险
- [ ] review-report.md 已写入

**On modified**: 更新 proposal.md 中的修改建议，gate pass，继续。
**On rejected**: `review.status = failed`，`current_phase` 回退到 `propose`，注入拒绝原因作为上下文。
**专家意见不一致时**: 以安全 > 架构 > 产品 > AI工程的优先级裁决（安全否决权）。

#### Phase: Spec

**Action**: Create/update `.claude/changes/<feature-name>/spec.md` using the template at `.claude/workflow/templates/change-spec.md`.

Content must include:
- Overview tied to proposal goals + review feedback
- Architecture view (which components interact)
- API specs (endpoints, request/response shapes)
- Data model (entities, tables, relationships)
- State machine changes (if applicable)
- Error scenarios and handling
- Performance targets

Research existing code before writing:
- Read `CLAUDE.md` for service map and patterns
- Read relevant `wiki/` files for domain context
- Check existing entities in `schemaplexai-model/`
- Check existing controllers in `schemaplexai-web/`
- Incorporate review feedback from `review-report.md`

**Quality Gate**: `spec-gate`
- [ ] spec.md exists and references proposal.md
- [ ] Review feedback addressed in spec
- [ ] API specs have request/response shapes
- [ ] Data model matches existing BaseEntity pattern
- [ ] Error scenarios are enumerated (not "TBD")
- [ ] Performance targets are numeric
- [ ] No TBD/TODO placeholders remain

**On fail**: Re-run with failure context.

#### Phase: Design

**Action**: Create/update `.claude/changes/<feature-name>/design.md` and `context.md`.

Content must include:
- C4 component diagram (mermaid)
- Module boundary decisions (aligned with review architecture feedback)
- Data flow description
- Deployment considerations
- If architecture decision is new: create ADR in `docs/decisions/`

Also update `context.md` with:
- Related modules
- Key files to modify
- Decision log entries

Research before designing:
- Check existing module dependencies in pom.xml files
- Verify service ports and prefixes from CLAUDE.md
- Check existing patterns (BaseEntity, BaseMapperX, BaseController)

**Quality Gate**: `design-gate`
- [ ] design.md exists with component diagram
- [ ] Module boundaries match existing service map
- [ ] Review architecture feedback incorporated
- [ ] No new architectural decisions left unjustified
- [ ] context.md updated with key files

**On fail**: Re-run with failure context.

#### Phase: Plan

**Action**: Create/update `.claude/changes/<feature-name>/tasks.md` using the template at `.claude/workflow/templates/change-tasks.md`.

Content must include:
- Mermaid task dependency graph
- Parallel execution groups
- Each task with: ID, files, type, description, acceptance criteria, estimated hours, dependencies, status
- Critical path analysis
- Quality gate checklist

Task decomposition rules:
- Each task ≤ 4 hours
- Each task has specific acceptance criteria (not "implementation is complete")
- Parallel groups identified for multi-agent execution
- File paths must be exact (not "path/to/file")

**Quality Gate**: `plan-gate`
- [ ] tasks.md exists with mermaid dependency graph
- [ ] All tasks have specific acceptance criteria
- [ ] No task exceeds 4 hours estimate
- [ ] Parallel groups identified
- [ ] File paths are exact
- [ ] Quality gate checklist present

**On fail**: Re-run with failure context.

#### Phase: Apply

**Action**: Execute tasks from `tasks.md` using TDD.

Process:
1. Read `tasks.md` and `spec.md`
2. For each task in dependency order:
   a. Write failing tests first (RED)
   b. Implement minimal code to pass (GREEN)
   c. Refactor if needed (REFACTOR)
   d. Mark task complete in tasks.md
3. For parallel task groups: use multi-agent delegation
4. Record all files changed in state file

Sub-agent rule: Every executor sub-agent MUST read `spec.md` before executing.

Environment checks before coding:
- `JAVA_HOME` → JDK 21
- Target module `pom.xml` includes `schemaplexai-dao` and required starters
- Frontend: `npm run lint` passes in `schemaplexai-ui/`

**Quality Gate**: `apply-gate`
- [ ] All tasks in tasks.md marked completed
- [ ] All acceptance criteria verified
- [ ] Tests exist for new functionality
- [ ] `mvn test` passes (backend changes)
- [ ] `npm run test:run` passes (frontend changes)

**On fail**: Fix failures, re-verify. Loop until gate passes.

#### Phase: Deliver

**Action**: 验证交付 — 跑完整测试套件 + 安全扫描 + 代码评审，确保可交付。

Process:
1. **Full test suite**:
   - `mvn clean test` (all backend modules)
   - `npm run test:run` (frontend, if applicable)
   - Read output, confirm all pass

2. **Verification gates** (if > 30 lines changed):
   - `/verify-change` — impact analysis, doc sync
   - `/verify-quality` — complexity, code smells, naming
   - `/verify-security` — vulnerability scan

3. **Code Review**:
   - `code-reviewer` agent for general quality
   - `security-reviewer` agent if security-sensitive code (auth, input handling, tenant isolation)

4. **CI gate simulation**:
   - `mvn jacoco:check` — coverage ≥ 80%
   - `npm run lint` — frontend lint

5. **Write delivery report**: `.claude/changes/<feature-name>/delivery-report.md`
   - Tests: pass/fail count
   - Coverage: percentage
   - Review findings: CRITICAL/HIGH/MEDIUM/LOW
   - Verification results

**Quality Gate**: `deliver-gate`
- [ ] All tests pass (`mvn test` + `npm run test:run`)
- [ ] Coverage ≥ 80% (jacoco:check)
- [ ] No CRITICAL or HIGH review findings
- [ ] verify-change passed (if > 30 lines)
- [ ] verify-quality passed (if > 30 lines)
- [ ] verify-security passed (security-sensitive code)
- [ ] delivery-report.md written

**On CRITICAL finding**: Must fix before gate pass. Re-apply fix, re-run deliver.
**On HIGH finding**: Should fix. If accepted as risk, document justification in delivery-report.md.
**On fail**: Fix issues, re-run deliver gate.

#### Phase: Archive

**Action**: Archive the completed change.

Process:
1. Run `.claude/workflow/scripts/change-archive.sh <feature-name>`
2. Sync: `spec.md` → `docs/specs/`, `design.md` → `docs/designs/`
3. Update `wiki/log.md` with: what was built, what was learned, what changed
4. Update `wiki/gaps.md` if new undocumented areas discovered
5. Update `wiki/active-areas.md` if hotspots changed
6. Commit all changes with conventional commit message
7. **合并 Reflexion 评分报告**: 收集 `.claude/changes/<feature>/judge-report-*.md`，合并为 `docs/archive/<feature>-<date>/reflexion-report.md`，包含：
   - 每个阶段的 Judge 评分
   - 总体加权平均分
   - 主要改进项和已解决项

**Quality Gate**: `archive-gate`
- [ ] Archive script executed successfully
- [ ] docs/ synced with spec and design
- [ ] wiki/log.md updated
- [ ] All changes committed
- [ ] reflexion-report.md 已写入 archive

**On fail**: Fix and re-archive.

### Step 2.5: LLM-as-Judge Quality Gate (CEK Pattern)

每个阶段执行完毕后，调用 Judge 子代理评分。评分维度和阈值来自 CEK Reflexion 模式。

**评分维度与权重：**

| 维度 | 权重 | 说明 |
|------|------|------|
| 指令遵循 (Instruction Following) | 30% | 是否严格按 spec/plan/模板执行 |
| 输出完整性 (Output Completeness) | 25% | 所有需求是否覆盖 |
| 方案质量 (Solution Quality) | 25% | 代码/文档质量、测试覆盖 |
| 推理质量 (Reasoning Quality) | 10% | 决策是否有理据 |
| 响应连贯性 (Response Coherence) | 10% | 文档/代码风格一致 |

**阶段最低评分阈值：**

| 阶段 | 最低加权分 | 关键检查项 |
|------|-----------|-----------|
| Propose → Review | 3.5 | 范围清晰、需求明确、无 TBD |
| Review → Spec | 3.5 | 所有 Critical issue 已解决 |
| Spec → Design | 4.0 | 架构合理、边界清晰、API 完整 |
| Design → Plan | 3.5 | 任务 ≤ 4h、有验收标准 |
| Plan → Apply | 3.5 | 每个 task 有验收标准、并行组识别 |
| Apply → Deliver | 3.5 | 测试通过、代码质量达标 |
| Deliver → Archive | 4.0 | 无 CRITICAL/HIGH、Reflexion 评分完成 |

**Judge 执行流程：**

```
1. 读取当前阶段产出文件（proposal.md / spec.md / design.md / tasks.md / 代码）
2. 按 5 个维度逐项评分（1-5 分）
3. 计算加权总分
4. 如果总分 < 阶段阈值：
   a. 列出具体问题和改进建议
   b. 返回 gate_result: fail + 详细评分报告
5. 如果总分 ≥ 阶段阈值：
   a. 返回 gate_result: pass + 评分报告
6. 将评分报告写入 .claude/changes/<feature>/judge-report-<phase>.md
```

**Judge 子代理 Prompt 模板：**

```
Task(subagent_type="general-purpose", name="judge-<phase>",
  prompt="你是质量评审 Judge。严格评审以下产出。

评审维度（每项 1-5 分）：
1. 指令遵循 (30%): 是否严格按要求执行
2. 输出完整性 (25%): 所有需求是否覆盖
3. 方案质量 (25%): 质量、测试、可维护性
4. 推理质量 (10%): 决策是否有理据
5. 响应连贯性 (10%): 风格一致

默认分 2 分。必须有证据才能加分。5 分极其罕见 (<5%)。

输出格式：
## Judge Report — <phase>
| 维度 | 分数 | 证据 |
|------|------|------|
| 指令遵循 | X/5 | ... |
| 输出完整性 | X/5 | ... |
| 方案质量 | X/5 | ... |
| 推理质量 | X/5 | ... |
| 响应连贯性 | X/5 | ... |
加权总分: X.XX/5.0
Verdict: PASS/FAIL (阈值: X.X)

[产出内容]")
```

**评分报告持久化：**

每次 Judge 评分后，写入：
`.claude/changes/<feature>/judge-report-<phase>.md`

Archive 阶段将所有 Judge 报告合并为 Reflexion 评分总报告：
`docs/archive/<feature>-<date>/reflexion-report.md`

### Step 3: Evaluate Gate

After executing the phase:
1. Run the quality gate checklist
2. Update state file with `gate_result: pass` or `gate_result: fail`
3. If pass: set `status: passed`, advance `current_phase` to next pending phase
4. If fail: set `status: failed`, record failure notes, keep `current_phase`
5. Special: Review rejected → set `review.status = failed`, set `current_phase = propose`

**附加检查**：如果 Step 2.5 的 Judge 评分报告存在且 Verdict=FAIL，即使 checklist 全部通过，gate 也判定为 fail。Judge 评分是硬性门禁，不可跳过。

### Step 4: Check Completion

1. Read state file
2. Are ALL phases `passed` or `skipped`?
3. If YES → Step 5 (Complete)
4. If NO → Loop back to Step 1

### Step 5: Complete

1. Report final summary:
   - Feature name
   - Phases completed (passed/skipped count)
   - Roundtable verdict
   - Files changed
   - Tests passing
   - Coverage
   - Total iterations
2. Clean up: remove `.claude/workflow-loop.local.md`
3. Output: `<promise>WORKFLOW COMPLETE</promise>`

## Escalation Rules

- Stop and report if a fundamental blocker requires user input (missing credentials, unclear requirements, external service down)
- If the same gate fails 3+ times, escalate with specific blocking issue
- If iteration exceeds `max_iterations`, report current state and stop
- If roundtable rejects 3 times, escalate to user for manual intervention
- Never output `<promise>WORKFLOW COMPLETE</promise>` unless ALL phases are genuinely passed

## Tool Usage

- `Task(subagent_type="planner", ...)` for Propose phase research
- `Task(subagent_type="general-purpose", ...)` × 4 for Review roundtable
- `Task(subagent_type="architect", ...)` for Design phase review
- `Task(subagent_type="executor", ...)` for Apply phase TDD execution
- `Task(subagent_type="code-reviewer", ...)` for Deliver phase review
- `Task(subagent_type="security-reviewer", ...)` for Deliver phase security review
- `Task(subagent_type="build-error-resolver", ...)` when build fails
- Run `mvn test` / `npm run test:run` in background (`run_in_background: true`)

## Final Checklist

Before outputting completion promise:
- [ ] All non-skipped phases have `status: passed`
- [ ] Roundtable review passed (or was skipped)
- [ ] All acceptance criteria in tasks.md are verified
- [ ] Tests passing (mvn test / npm run test:run)
- [ ] Coverage ≥ 80%
- [ ] No CRITICAL/HIGH review findings
- [ ] Archive completed (or phase was skipped)
- [ ] No pending TODO items
- [ ] State file cleaned up
- [ ] All Judge reports collected in reflexion-report.md
- [ ] Overall weighted score ≥ 3.5

## Examples

<Good>
```
/workflow-loop agent-pause-resume Add pause/resume to agent execution

→ Initializes change workspace
→ Propose: writes proposal.md with scope, impact on agent-engine
→ Review: 4 experts review in parallel
  - Product: approved (high user value)
  - Architect: modified (建议拆分 pause service)
  - Security: approved (需加租户隔离检查)
  - AI Engineer: approved (状态机扩展合理)
  - Verdict: modified → proposal.md updated → gate pass
→ Spec: writes spec.md incorporating review feedback
→ Design: writes design.md with state machine diagram
→ Plan: writes tasks.md with 5 tasks (1 个拆分自 review 建议)
→ Apply: TDD execution, all tests pass
→ Deliver: mvn test pass, coverage 85%, code-reviewer clean, verify-security pass
→ Archive: syncs docs, updates wiki
→ Output: <promise>WORKFLOW COMPLETE</promise>
```
</Good>

<Bad>
```
/workflow-loop fix bug

→ No feature name, no description
→ Cannot initialize properly
```
Why bad: Missing required arguments.
</Bad>

<Bad>
```
/workflow-loop huge-refactor Refactor entire auth system across 5 modules

→ Skips Propose, Review, Spec, Design
→ Starts directly at Plan
```
Why bad: Cross-module changes > 200 lines must use all 8 phases including roundtable review.
</Bad>

<Bad>
Roundtable with 2+ Critical issues but gate passes:
```
→ Security: REJECTED (无输入验证，XSS 风险)
→ Architect: REJECTED (模块耦合违反服务边界)
→ Verdict: approved ❌
```
Why bad: 2+ 专家 Critical = 必须 rejected，退回 Propose。
</Bad>
