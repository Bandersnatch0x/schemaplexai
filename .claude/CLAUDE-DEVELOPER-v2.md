# CLAUDE-DEVELOPER-v2.md — Developer Workflow (OpenSpec + Superpowers)

> **When to use**: With Superpowers plugin installed (`/brainstorming`, `/writing-plans`, `/tdd`, `/verify`).
> **Full guide**: `.claude/workflow/GUIDE.md`

---

## Six-Phase Change Lifecycle

```
Propose → Review → Design → Build → Deliver → Archive
```

All changes execute in `.claude/changes/<feat>/`.

| Phase | Trigger | Output | Key Tool |
|-------|---------|--------|----------|
| **Propose** | `/opsx:propose "desc"` | `proposal.md` + `spec.md` (draft) | OpenSpec |
| **Review** | Propose done | Approved/Modified/Rejected | Human |
| **Design** | Review passed | `tasks.md` (Graphify graph) | `/brainstorming` → `/writing-plans` |
| **Build** | Design confirmed | Code + tests | `/tdd` + executor sub-agents |
| **Deliver** | Build done | Verified artifact | `/verify-*` + CI + Code Review |
| **Archive** | PR merged | `archive/` + `docs/` + `wiki/` | `/opsx:archive` |

### Phase Details

**Propose** (`/opsx:propose`): Generate `proposal.md` + `spec.md` draft (秘格) + `context.md`. Pre-warm context from `wiki/` and `docs/specs/`.

**Review**: Human reviews proposal and spec draft. Pass → Design; Modify → back to Propose; Reject → archive to `archive/<feat>-rejected/`.

**Design**:
- `/brainstorming`: Explore implementation options → `notes/brainstorming.md`
- `/writing-plans`: Generate Graphify task graph → `tasks.md`. Replace Phase 1 draft. Each task ≤ 4h with acceptance criteria.

**Build** (`/tdd`):
- `git checkout -b feature/<feat>`. Use `EnterWorktree` for complex changes.
- Serial tasks: sequential execution. Parallel groups: OMC multi-agent.
- Every task: TDD RED→GREEN→REFACTOR.
- Sub-agents must read `.claude/changes/<feat>/spec.md` before executing.
- >30 lines: trigger `/verify-change`, `/verify-quality`, `/verify-security`.

**Deliver**:
- Self-test: unit + integration + manual golden path + boundary + multi-tenant
- Verification gate: `/verify-change`, `/verify-quality`, `/verify-security`, ≥80% coverage
- Code Review: `code-reviewer` agent (general), `security-reviewer` agent (security-sensitive)
- CI gate: `mvn test`, `mvn jacoco:check`, `npm run test:run`
- **Must pass all gates before Archive.**

**Archive** (`/opsx:archive`):
- Run `.claude/workflow/scripts/change-archive.sh <feat>`
- Sync: `spec.md` → `docs/specs/`, `design.md` → `docs/designs/`, decisions → ADR
- Knowledge backflow: `wiki/log.md`, `wiki/gaps.md`

## Command Quick Reference

| Command | Phase | Tool | Action |
|---------|-------|------|--------|
| `/opsx:propose "desc"` | Propose | OpenSpec | Init change, generate proposal + spec draft |
| `/brainstorming` | Design | Superpowers | Explore implementation options |
| `/writing-plans` | Design | Superpowers | Generate Graphify task graph |
| `/tdd` | Build | Superpowers | TDD execution |
| `/verify-change` | Build/Deliver | Superpowers/CCG | Impact analysis |
| `/verify-quality` | Build/Deliver | Superpowers/CCG | Complexity/code smells |
| `/verify-security` | Build/Deliver | Superpowers/CCG | Security scan |
| `/opsx:archive` | Archive | OpenSpec | Archive change, sync specs |
| `.claude/workflow/scripts/change-status.sh` | Any | Custom | Show active changes |

## Layered Context

```
Layer 1: CLAUDE.md                  ← Project constitution (always loaded)
Layer 2: CLAUDE-DEVELOPER-v2.md     ← This file — workflow protocol
Layer 3: wiki/*.md                  ← Domain knowledge (load on demand)
Layer 4: .claude/changes/<feat>/spec.md    ← Task spec context (sub-agents must read)
Layer 5: .claude/changes/<feat>/context.md  ← Task execution context
```

**Rule**: Every executor sub-agent must read Layer 4 (`spec.md`) before executing Build tasks.

## Version Selection

| Scenario | Use |
|----------|-----|
| Superpowers + OpenSpec installed | **This file (v2)** |
| Generic agent only | `.claude/CLAUDE-DEVELOPER.md` (v1) |
| In-progress v1 change | Continue v1, no migration needed |

## Standards Mapping

| This Workflow | Standard Document |
|--------------|------------------|
| Propose + Review | `docs/standards/feature-workflow.md` Phase 1-3 |
| Design | `docs/standards/feature-workflow.md` Phase 4-7 |
| Build + Deliver | `docs/standards/feature-workflow.md` Phase 8-9 |
| Archive | `docs/standards/feature-workflow.md` Phase 10 |
| TDD | `docs/standards/tdd-guide.md` |
| Review | `docs/standards/review-checklists.md` |
