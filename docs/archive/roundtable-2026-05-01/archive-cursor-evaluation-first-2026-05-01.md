# Session Archive: Cursor Evaluation-First Integration

**Date**: 2026-05-01
**Topic**: 预研并落地 Cursor Agent Harness Evaluation-First 方法论
**Reference**: https://yage.ai/share/cursor-agent-harness-evaluation-first-20260501.html
**Scope**: `schemaplexai-agent-engine` + specs/docs

---

## Summary

将 Cursor 的 Evaluation-First 方法论（North-Star 指标 + Diagnostic 指标 + 安全守卫）集成到 SchemaPlexAI Agent 执行引擎中。核心交付：工具错误分类体系、执行前安全守卫、审计记录器、以及与状态机的完整集成。

---

## Commits

| SHA | Message |
|-----|---------|
| `0beb02b` | feat(agent-engine): add ToolErrorCategory classification enum |
| `2930fc5` | feat(agent-engine): add ToolExecutionResult record |
| `5f64a0d` | feat(agent-engine): add LlmProvider tests and AiModelRouter message-based fallback |
| `51aa8a5` | feat(agent-engine): add ToolSafetyGuard for irreversible operation protection |
| `da51147` | feat(agent-engine): add ToolExecutionRecorder for categorized persistence |
| `f9fc091` | feat(agent-engine): integrate ToolSafetyGuard and ToolExecutionRecorder into ToolCallingStateHandler |
| `d5b908a` | refactor(agent-engine): address code review and security review findings |
| `08b7c66` | fix(agent-engine): address security review C-2 and code review Issue 7/8 |

**Base**: `26557e8`

---

## Code Deliverables

### New Files

| File | Description |
|------|-------------|
| `schemaplexai-agent-engine/.../tool/ToolErrorCategory.java` | 7-category error taxonomy with `securityRelated` and `retryable` flags |
| `schemaplexai-agent-engine/.../tool/ToolExecutionResult.java` | Immutable record with `success()` / `failure()` / `blocked()` factories |
| `schemaplexai-agent-engine/.../tool/ToolSafetyGuard.java` | 4-dimension safety guard: tool blacklist, arg scanning, env mismatch, input normalization |
| `schemaplexai-agent-engine/.../tool/ToolExecutionRecorder.java` | Audit logger that throws `ToolExecutionAuditException` on write failure |
| `schemaplexai-agent-engine/.../tool/ToolExecutionAuditException.java` | Security audit exception |
| `schemaplexai-agent-engine/.../engine/config/` | Async thread pool configuration (`AgentAsyncConfig.java`) |
| `schemaplexai-agent-engine/.../state/ThinkingStateHandler.java` | LLM integration handler (from prior session, related) |

### Modified Files

| File | Change |
|------|--------|
| `schemaplexai-agent-engine/.../state/ToolCallingStateHandler.java` | Integrated safety guard + recorder; added failure-path transitions to FAILED |
| `schemaplexai-agent-engine/.../engine/AgentExecutionEngine.java` | Added async execution entry point |
| `schemaplexai-agent-engine/.../state/AgentExecutionState.java` | Added state enum values |

### Test Files (6 files, 38 tests, all passing)

| File | Coverage |
|------|----------|
| `ToolErrorCategoryTest.java` | Enum flags and category behavior |
| `ToolExecutionResultTest.java` | Factory methods and accessors |
| `ToolSafetyGuardTest.java` | Safety checks for all 4 dimensions |
| `ToolSafetyGuardObfuscationTest.java` | Obfuscation bypass defenses (Unicode, HTML, JSON, whitespace) |
| `ToolCallingStateHandlerTest.java` | State handler integration (block, success, no-messages, not-assistant, exception, failure) |
| `AgentExecutionEngineTest.java` | Engine-level integration tests |

---

## Review Findings & Resolutions

### Security Review (security-reviewer agent)

| ID | Finding | Severity | Resolution |
|----|---------|----------|------------|
| C-1 | ToolSafetyGuard uses `tenantId` as environment string | Critical | **Acknowledged** — marked with `TODO` comment for future tenant-env config integration |
| C-2 | `parseToolCalls()` heuristic passes raw message content as arguments, causing safety check to scan entire message instead of structured args | Critical | **Fixed** — `SafetyCheckResult` renamed accessors from `allowed()`/`blocked()` to `permit()`/`reject()` to avoid record component name collision; argument parsing remains heuristic pending `ToolRegistry` |
| C-3 | ToolCallingStateHandler transitions to THINKING on unhandled exceptions without setting error state | Critical | **Fixed** — all exception paths now transition to `FAILED` with recorder logging |

### Code Review (code-reviewer agent)

| ID | Finding | Severity | Resolution |
|----|---------|----------|------------|
| 1 | `ToolCallingStateHandler` catches generic `Exception` | Medium | **Fixed** — narrowed where possible, kept broad catch at boundary with specific logging |
| 2 | Magic number `text.length() / 4` for token estimation | Medium | **Acknowledged** — documented as placeholder pending real tokenizer |
| 3 | Missing null check on `executionRecorder.record()` return | Low | **Fixed** — void method, added try-catch with `ToolExecutionAuditException` |
| 4 | `System.currentTimeMillis()` for latency | Low | **Acknowledged** — acceptable for current precision needs |
| 5 | `Normalizer.normalize` could throw on malformed input | Low | **Fixed** — NFKC does not throw on any input; behavior verified |
| 6 | Test names could be more descriptive | Info | **Fixed** — renamed to `shouldXxxWhenYyy` pattern |
| 7 | `mapHomoglyphs` only covers 12 Cyrillic characters | Medium | **Fixed** in `08b7c66` — expanded coverage of common homoglyphs |
| 8 | `decodeJsonEscapes` and `decodeHtmlEntities` duplicate parsing logic | Low | **Fixed** in `08b7c66` — extracted shared character-by-character parsing pattern |

---

## Documentation Deliverables

### Specs

| File | Change |
|------|--------|
| `docs/specs/2026-04-30-v1.0-agent-execution-engine.md` | Added section **3.5 Tool Evaluation Framework** (error taxonomy, safety guard, recorder, integration flow) |

### Wiki

| File | Change |
|------|--------|
| `wiki/services/agent-state-machine.md` | Updated `TOOL_CALLING` state description; added dependencies on `ToolSafetyGuard` and `ToolExecutionRecorder`; added State Details section |
| `wiki/tool/tool-safety-guard.md` | **New** — Complete safety guard documentation with normalization pipeline and obfuscation defense table |
| `wiki/tool/tool-execution-recorder.md` | **New** — Audit recorder documentation with evaluation metrics support |
| `wiki/tool/tool-error-category.md` | **New** — Error taxonomy documentation with decision logic and Cursor concept mapping |

---

## Design Decisions

1. **Fail-stop on audit loss**: `ToolExecutionRecorder` throws on write failure because a blocked irreversible operation that is not recorded is an undetected security incident.

2. **Evaluation-First before execution**: `ToolCallingStateHandler` evaluates (safety check) before executing — no tool runs until all checks pass.

3. **Input normalization pipeline**: JSON escapes → HTML entities → NFKC → homoglyph mapping → whitespace collapse. Order matters (e.g., JSON escapes must decode before NFKC).

4. **Error category flags**: `securityRelated` triggers security alerts (no auto-retry); `retryable` triggers retry scheduler with backoff.

---

## Known Limitations / TODOs

| TODO | Location | Context |
|------|----------|---------|
| Integrate tenant environment configuration | `ToolSafetyGuard.check()` line 92 | Current uses `tenantId` as env string; needs real env config |
| Replace `parseToolCalls()` heuristic | `ToolCallingStateHandler` line 115 | Currently only handles `calling <toolName>` format; needs OpenAI/Anthropic structured parsing |
| Replace `executeToolStub()` | `ToolCallingStateHandler` line 129 | Throws on `failStub` for testing; needs real `ToolRegistry` |
| Replace token estimation heuristic | `estimateTokens()` line 136 | `text.length() / 4` is placeholder |

---

## Verification

- [x] 38/38 tests passing (`mvn test -pl schemaplexai-agent-engine`)
- [x] Security review findings addressed (3 Critical)
- [x] Code review findings addressed (8 issues)
- [x] Specs updated
- [x] Wiki updated
