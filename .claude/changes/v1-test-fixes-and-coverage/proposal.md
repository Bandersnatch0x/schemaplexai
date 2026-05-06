---
change_id: v1-test-fixes-and-coverage
status: draft
created_at: 2026-05-05
---

# Proposal: Fix Pre-existing Test Failures + Add Coverage

## 1. Problem Statement

The v1-release-readiness workflow achieved 96% test pass rate (506/527). The remaining 21 failures/errors in schemaplexai-agent-engine are all pre-existing expectation mismatches between tests and source code. These must be resolved before v1.0 release.

## 2. Root Cause Analysis

### Category A: Missing @Mock Declarations (10 tests)

| Test | Missing Mock | Impact |
|------|-------------|--------|
| ThinkingStateHandlerTest (5 failures) | `AgentLoopDetectionService` | NPE → transitions to FAILED instead of COMPLETED/TOOL_CALLING |
| ToolCallingStateHandlerTest (2 failures + 1 error) | `ToolRegistry`, `ToolSandbox`, `AgentLoopDetectionService`, `SecurityPolicyLoader` | NPE at `toolRegistry.parse()` → UNEXPECTED_ENVIRONMENT |

### Category B: Test/Source Mismatch (8 tests)

| Test | Mismatch | Fix |
|------|----------|-----|
| FinalAnswerExtractorTest (2 failures) | THOUGHT_PATTERN regex missing `Thought` in lookahead | Add `Thought` to lookahead group |
| MemoryStrategyTest (1 failure) | `estimateTokens()` uses content.length()/4, not `tokensPerMessage` | Adjust test to match actual estimation logic |
| ExceptionHandlingStateHandlerTest (5 failures + 1 error) | (1) `List.of()` is immutable, `.add()` throws UnsupportedOperationException (2) `getCurrentState()` guard blocks RETRY transition | (1) Use `new ArrayList<>(list)` in constructor (2) Stub `getCurrentState()` in tests |

### Category C: Source Code Ordering (1 test)

| Test | Issue | Fix |
|------|-------|-----|
| ToolSandboxTest (1 failure) | `execute()` checks whitelist before validation; empty tool name hits PERMISSION_DENIED before INVALID_ARGUMENT | Move validation before whitelist check |

### Category D: Spring Context Loading (3 errors)

| Test | Issue | Fix |
|------|-------|-----|
| ObservabilityRecorderTest (2 errors) | `@SpringBootTest` without Redis/RabbitMQ exclusion | Add autoconfigure exclusions |
| AgentRuntimeOrchestratorIntegrationTest (1 error) | Same | Same |

## 3. Fix Strategy

All fixes are test-side or minor source corrections. No architectural changes.

### Source Fixes (4 files):
1. `FinalAnswerExtractor.java` — Fix THOUGHT_PATTERN regex
2. `ExceptionHandlingStateHandler.java` — Use mutable list in constructor
3. `ContainerToolSandbox.java` — Move validation before whitelist check
4. `SlidingWindowStrategy.java` — No change needed (test fix instead)

### Test Fixes (6 files):
1. `ThinkingStateHandlerTest.java` — Add `@Mock AgentLoopDetectionService`
2. `ToolCallingStateHandlerTest.java` — Add missing `@Mock` declarations
3. `ExceptionHandlingStateHandlerTest.java` — Stub `getCurrentState()`, fix immutable list test
4. `MemoryStrategyTest.java` — Adjust expectations to match content-length estimation
5. `ToolSandboxTest.java` — No change needed (source fix)
6. `ObservabilityRecorderTest.java` — Add Spring context exclusions
7. `AgentRuntimeOrchestratorIntegrationTest.java` — Add Spring context exclusions

### Coverage Addition:
- Add JaCoCo Maven plugin to parent pom.xml
- Target: 80%+ line coverage for core modules

## 4. Scope

**In scope:**
- Fix all 21 test failures/errors
- Add JaCoCo coverage configuration
- Verify `mvn clean test` passes (0 failures)

**Out of scope:**
- CI/CD pipeline setup (separate task)
- Integration tests (separate task)
- API documentation (separate task)

## 5. Success Criteria

- [ ] `mvn clean test` passes with 0 failures across all modules
- [ ] JaCoCo reports generated for core modules
- [ ] No regression in passing tests

## 6. Risk Assessment

- **Low risk**: All fixes are correcting test expectations to match actual source behavior, or minor source improvements (regex fix, list mutability, validation ordering)
- **No breaking changes**: All fixes preserve existing API contracts
