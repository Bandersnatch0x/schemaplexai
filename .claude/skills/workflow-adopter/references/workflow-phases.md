# Workflow Phase Reference (8 Phases)

Quick reference for the SchemaPlexAI change lifecycle. Load on demand when SKILL.md needs detail.

## Phase Quick Reference

| Phase | Output | Template | Gate | Type |
|-------|--------|----------|------|------|
| Propose | `proposal.md` | `change-proposal.md` | problem specific, scope explicit | Auto |
| Review | `review-report.md` | — | 4-expert roundtable verdict | Parallel Agents |
| Spec | `spec.md` | `change-spec.md` | API shapes, data model, error scenarios | Auto |
| Design | `design.md` | `change-design.md` | C4 diagram, module boundaries | Auto |
| Plan | `tasks.md` | `change-tasks.md` | dependency graph, acceptance criteria | Auto |
| Apply | Code + tests | — | all tasks done, tests pass | TDD |
| Deliver | `delivery-report.md` | — | full test + review + verify gates | Agents + CI |
| Archive | `archive/` + `docs/` + `wiki/` | — | script ran, docs synced | Script |

## Roundtable Review (Phase 2)

4 位专家并行审查 proposal.md：

| 专家 | 视角 | 关注点 | 否决权 |
|------|------|--------|--------|
| Product | 产品 | 用户价值、ROI、范围合理性 | 无 |
| Architect | 架构 | 模块边界、依赖、可扩展性、技术债务 | 有（架构违规） |
| Security | 安全 | OWASP、数据隔离、认证授权、租户安全 | **最高否决权** |
| AI Engineer | AI工程 | LLM集成、Token管理、状态机、可观测性 | 无 |

**Verdict rules**:
- Approved: 4/4 或 3/4 同意，无 Critical
- Modified: 有建议但无阻断 → 更新 proposal 继续
- Rejected: 2+ Critical → 退回 Propose

**裁决优先级**: Security > Architect > Product > AI Engineer

## Deliver Phase (Phase 7)

验证交付门禁：

| 检查项 | 命令 | 条件 |
|--------|------|------|
| Backend 测试 | `mvn clean test` | 有 Java 变更 |
| Frontend 测试 | `npm run test:run` | 有前端变更 |
| 覆盖率 | `mvn jacoco:check` | ≥ 80% |
| 变更影响 | `/verify-change` | > 30 行 |
| 代码质量 | `/verify-quality` | > 30 行 |
| 安全扫描 | `/verify-security` | 安全敏感代码 |
| 代码评审 | `code-reviewer` agent | 始终 |
| 安全评审 | `security-reviewer` agent | auth/input/tenant |

## Skip Rules

| Scenario | Skip | Must Keep |
|----------|------|-----------|
| < 50 lines | Propose, Review, Spec, Design | Plan + Apply + Deliver + Archive |
| Bug fix | Propose, Review | Spec + Plan + Apply + Deliver + Archive |
| Frontend UI only | Design | Spec (simplified) + Plan + Apply + Deliver |
| > 200 lines or cross-module | — | All 8 phases |
| New service/module | — | All 8 phases + ADR |

## Service Map (from CLAUDE.md)

| Service | Port | Prefix | Role |
|---------|------|--------|------|
| gateway | 8080 | — | JWT, tenant, rate limit |
| system | 8081 | `/system/**` | Tenant, user, role |
| web | 8082 | `/web/**` | Controllers, SSE, WS |
| agent-config | 8083 | `/agent-config/**` | Agent definitions |
| agent-engine | 8084 | `/agent/**` | LLM orchestration |
| context | 8085 | `/context/**` | RAG, vector search |

## Key Patterns

- Entities extend `BaseEntity`
- Mappers extend `BaseMapperX<T>`
- Controllers extend `BaseController` with `success()` / `error()`
- All endpoints return `Result<T>`
- Multi-tenant: `X-Tenant-Id` header → `TenantContextHolder`

## Agent Roles

| Phase | Agent | Why |
|-------|-------|-----|
| Propose | `planner` | Scope, risk |
| Review | 4 × `general-purpose` | Roundtable multi-perspective |
| Design | `architect` | C4, boundaries |
| Apply | `executor` (opus) | Complex implementation |
| Deliver | `code-reviewer` + `security-reviewer` | Quality + security gates |
| Debug | `build-error-resolver` | Build fixes |
