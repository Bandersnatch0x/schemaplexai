# CLAUDE-DEVELOPER.md — Developer Workflow (v1, Generic Agent Mode)

> **When to use**: Default workflow when Superpowers plugin is unavailable.
> **Full guide**: `.claude/workflow/GUIDE.md`

---

## Six-Phase Change Lifecycle

```
Propose → Spec → Design → Plan → Apply → Archive
```

All changes execute in `.claude/changes/<feat>/`.

| Phase | Trigger | Output | Gate |
|-------|---------|--------|------|
| **Propose** | "I want to build X" | `proposal.md` | Developer confirms scope |
| **Spec** | Proposal confirmed | `spec.md` | >200 lines: human review; <200: self-review |
| **Design** | Spec confirmed + architecture change | `design.md` | `architect` agent review |
| **Plan** | Spec/Design confirmed | `tasks.md` (Graphify graph) | Developer confirms task decomposition |
| **Apply** | Plan confirmed | Code + tests | `/verify-*` for >30 lines; TDD RED→GREEN→REFACTOR |
| **Archive** | PR merged | `.claude/changes/archive/` + updated `docs/` + `wiki/` | — |

### Phase Details

**Propose**: Create `.claude/changes/<feat>/proposal.md` with problem, goal, scope, impact assessment. Skip for <50 lines.

**Spec**: Write `.claude/changes/<feat>/spec.md` with: overview, architecture view, API specs, data model, state machine (if any), error scenarios, performance targets. Sync to `docs/specs/<topic>.md` for public contract changes.

**Design**: Write `.claude/changes/<feat>/design.md` with C4 diagrams, module boundaries, data flow, deployment considerations. Create ADR if new architectural decision.

**Plan**: Decompose into Graphify task graph in `.claude/changes/<feat>/tasks.md`. Each task ≤ 4 hours, with acceptance criteria. Identify parallel task groups.

**Apply**: `git checkout -b feature/<feat>`. Use `EnterWorktree` for complex changes. Execute tasks: parallel independent groups via OMC multi-agent, serial tasks sequentially. TDD for every task.

**Archive**: Run `.claude/workflow/scripts/change-archive.sh <feat>`. Move to `archive/`. Sync spec/design to `docs/`. Update `wiki/log.md` and `wiki/gaps.md`.

## Command Quick Reference

| Command | Phase | Action |
|---------|-------|--------|
| `/propose <desc>` | Propose | Initialize change |
| `/spec` | Spec | Generate spec from proposal |
| `/design` | Design | Generate architecture design |
| `/plan` | Plan | Decompose to Graphify task graph |
| `/apply` | Apply | Execute tasks |
| `/status` | Any | Show active changes |
| `/archive <feat>` | Archive | Archive completed change |

## Layered Context

```
Layer 1: CLAUDE.md              ← Project constitution (always loaded)
Layer 2: CLAUDE-DEVELOPER.md    ← This file — workflow protocol
Layer 3: wiki/*.md              ← Domain knowledge (load on demand)
Layer 4: .claude/changes/<feat>/spec.md    ← Task spec context (sub-agents must read)
Layer 5: .claude/changes/<feat>/context.md  ← Task execution context
```

**Rule**: Every executor sub-agent must read Layer 4 (`spec.md`) before executing Build tasks.

## Standards Mapping

| This Workflow | Standard Document |
|--------------|------------------|
| Propose + Spec | `docs/standards/feature-workflow.md` Phase 1-3 |
| Design | `docs/standards/feature-workflow.md` Phase 4-5 |
| Plan | `docs/standards/feature-workflow.md` Phase 6-7 |
| Apply | `docs/standards/feature-workflow.md` Phase 8-9 |
| Archive | `docs/standards/feature-workflow.md` Phase 10 |
| TDD | `docs/standards/tdd-guide.md` |
| Review | `docs/standards/review-checklists.md` |
