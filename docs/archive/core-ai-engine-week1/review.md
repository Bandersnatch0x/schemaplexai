# Core AI Engine Week 1 Code Review Report

**Review Date**: 2026-05-01
**Reviewer**: Code Review Agent
**Commits Reviewed**: `7f180ab`, `3ff6fc5`, `5f64a0d`
**Scope**: @Async thread pool, ThinkingStateHandler, LlmProvider / AiModelRouter
**Reference**: `spec.md` + `design.md` in `.claude/changes/core-ai-engine-design/`

---

## Executive Summary

Overall quality is **solid** for a Week 1 scaffolding phase. All 3 commits align well with the Phase 2 spec/design. Test coverage is good (35 new tests across 6 test classes). No CRITICAL security issues found. Two HIGH issues require attention before Week 2 integration, and several MEDIUM/LOW items should be addressed in follow-up commits.

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0 | - |
| HIGH | 2 | Must fix before merge |
| MEDIUM | 4 | Should fix in next iteration |
| LOW | 5 | Nice to have |

---

## What Was Done Well

1. **Clean separation of concerns**: `AgentExecutionEngine` delegates to `AgentRuntimeOrchestrator` via `@Async`, `ThinkingStateHandler` is focused on LLM interaction logic only.
2. **Defensive programming in ThinkingStateHandler**: null checks on `messages.isEmpty()`, `text == null`, `response.isBlank()`, try-catch wrapping the entire handler.
3. **TokenBudget output overdraft protection**: The 3ff6fc5 commit correctly added output token budget check (previously only input was checked), with proper `GATE_BLOCKED` transition.
4. **Fallback chain in AiModelRouter**: Both `generateWithFallback` and `generateWithMessages` have identical fallback logic with cooldown activation on failure.
5. **Graceful shutdown configured**: `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds(60)` in thread pool.
6. **Test naming is descriptive**: Tests follow the `methodNameShouldDoX` pattern, making intent clear.
7. **No hardcoded secrets**: API keys are not present in source; providers are stubs returning empty strings.

---

## CRITICAL (0)

None found.

---

## HIGH (2)

### HIGH-1: `AgentExecutionEngineTest` tests `@Async` method synchronously — test is misleading

**File**: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/AgentExecutionEngineTest.java`
**Line**: 68-80 (`startExecutionShouldTriggerAsyncOrchestrator`)

**Issue**: The test `startExecutionShouldTriggerAsyncOrchestrator` verifies that `orchestrator.run()` is called, but because `@Async` is not actually asynchronous in a plain Mockito unit test (no Spring context to proxy the `@Async` annotation), the call happens synchronously on the test thread. The test name implies async behavior is verified, but it only verifies delegation.

**Impact**: Test gives false confidence about async behavior. Thread pool exhaustion, rejection policy behavior, and actual async execution semantics are untested.

**Recommendation**:
- Rename test to `startExecutionShouldDelegateToOrchestrator` to match actual assertion.
- Add an integration test with `@SpringBootTest` that verifies the method returns before `orchestrator.run()` completes (e.g., using `CountDownLatch` inside a test double).
- Alternatively, use `AopTestUtils` to verify the proxy is present:

```java
@Test
void startExecutionShouldUseAsyncProxy() {
    assertTrue(AopUtils.isAopProxy(executionEngine),
        "ExecutionEngine should be a Spring proxy for @Async to work");
}
```

**Spec Alignment**: Section 4.1 design.md states "@Async(\"agentExecutionExecutor\")" — the implementation is correct, but test coverage for the async aspect is insufficient.

---

### HIGH-2: `AgentExecutionAsyncConfigTest` is a `@SpringBootTest` that only loads a single `@Configuration` class — will fail in real Spring Boot context

**File**: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/config/AgentExecutionAsyncConfigTest.java`
**Line**: 12

**Issue**: `@SpringBootTest(classes = {AgentExecutionAsyncConfig.class})` loads only the config class. This test does NOT verify that the bean is correctly created in the actual application context (where `@EnableAsync` is processed by Spring's async infrastructure). The test passes but does not guarantee the executor works when the full application starts.

**Impact**: Configuration drift risk. If another bean conflicts with `agentExecutionExecutor` or if `@EnableAsync` is removed from the main app class, this test won't catch it.

**Recommendation**:
- Change to `@SpringBootTest` (no classes arg) so the full context loads, OR
- Add a second test class `AgentExecutionAsyncIntegrationTest` that loads the full context and verifies the executor bean is present and functional.
- Keep the focused test for parameter verification, but add an integration guard.

---

## MEDIUM (4)

### MEDIUM-1: `AiModelRouter.route()` caches provider by `preferredModelId` but never invalidates cache

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/model/AiModelRouter.java`
**Line**: 32

**Issue**: `providerCache.put(preferredModelId, provider)` caches the provider indefinitely. If a provider goes unhealthy after being cached, `route()` will still return the cached unhealthy provider on subsequent calls for the same model. The cache is a `ConcurrentHashMap` with no TTL or invalidation.

**Impact**: Stale provider selection. A model that was once routed to OpenAI will keep going to OpenAI even if OpenAI becomes unhealthy, bypassing the health check and cooldown logic.

**Recommendation**:
- Either remove the cache (simplest, providers list is small), OR
- Add cache invalidation on health check failure:

```java
public LlmProvider route(String preferredModelId) {
    LlmProvider cached = providerCache.get(preferredModelId);
    if (cached != null && cached.isHealthy() && !isOnCooldown(cached.getProviderName())) {
        return cached;
    }
    // ... existing logic ...
}
```

**Spec Alignment**: design.md Section 5 "LLM 接入" specifies "支持多 provider fallback" — stale cache breaks this guarantee.

---

### MEDIUM-2: `ThinkingStateHandler.handle()` catches ALL exceptions with a single `catch (Exception e)` — masks recoverable vs unrecoverable errors

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ThinkingStateHandler.java`
**Line**: 85-88

**Issue**: The blanket catch transitions to FAILED for everything. Per spec Section 7, certain exceptions should have different behaviors:
- Token budget exceeded → `GATE_BLOCKED` (already handled inline)
- LLM timeout → should be `RETRYING` (spec says "30s 无响应 → 标记失败", but RETRYING handler doesn't exist yet)
- Memory store transient failure → could potentially be retried
- Illegal state / programming errors → `FAILED` is correct

**Impact**: All failures look the same to callers. No differentiation for observability or user-facing error messages.

**Recommendation**:
- Catch specific exception types before the blanket catch:

```java
catch (IllegalStateException e) {
    log.error("Unrecoverable error in THINKING for execution {}", execution.getId(), e);
    stateMachine.transition(AgentExecutionState.FAILED, execution);
} catch (Exception e) {
    log.error("Thinking state failed for execution {}", execution.getId(), e);
    // TODO: distinguish retryable vs non-retryable when RetryingStateHandler exists
    stateMachine.transition(AgentExecutionState.FAILED, execution);
}
```

**Spec Alignment**: spec.md Section 7 exception matrix defines different behaviors for different scenarios.

---

### MEDIUM-3: `containsToolCalls()` uses fragile string matching — will false-positive on natural language

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ThinkingStateHandler.java`
**Line**: 109-120

**Issue**: The tool call detection checks for substring presence of `<tool>`, `<function>`, ````tool`, `TOOL_CALL`, `invoke_tool`. A user prompt like "How do I use the `<tool>` tag in XML?" or "Explain `invoke_tool` function" would false-positive as a tool call.

**Impact**: False transitions to `TOOL_CALLING` state, causing execution to get stuck or fail when no actual tool calls exist.

**Recommendation**:
- Use structured output parsing (JSON schema or XML parsing) instead of substring detection.
- At minimum, require the indicator to appear in an `assistant` role context, not just anywhere in the response.
- Consider using a regex that matches complete tags: `Pattern.compile("<tool>\s*(\w+)\s*</tool>")`.
- The unused constant `TOOL_DETECTION_THRESHOLD = 3` suggests the original intent was threshold-based detection — either implement it or remove the constant.

**Spec Alignment**: spec.md Section 4.1 shows tool calls as structured data, not free-text substrings.

---

### MEDIUM-4: `AgentExecutionState.isTerminal()` does not include `GATE_BLOCKED`

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentExecutionState.java`
**Line**: 17-19

**Issue**: `isTerminal()` returns true only for `COMPLETED`, `FAILED`, `CANCELLED`. `GATE_BLOCKED` is documented as a terminal state in spec.md Section 5.1 ("终端状态"), but `isTerminal()` returns false for it. This means `AgentStateMachine.transition()` (line 36) will allow transitions FROM `GATE_BLOCKED`, and `AgentRuntimeOrchestrator` loop (line 66) will not break on `GATE_BLOCKED`.

**Impact**: `GATE_BLOCKED` executions could be transitioned to other states incorrectly. Orchestrator loop may continue iterating.

**Recommendation**:

```java
public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED || this == GATE_BLOCKED;
}
```

**Spec Alignment**: spec.md Section 5.1 table explicitly marks `GATE_BLOCKED` as "终端".

---

## LOW (5)

### LOW-1: `estimateTokens()` uses naive `length / 4` heuristic — accuracy far from spec target

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ThinkingStateHandler.java`
**Line**: 102-107

**Issue**: The token estimation is extremely rough (1 token ~= 4 chars). For Chinese text, this is wildly inaccurate (1 Chinese character ~= 1-2 tokens). The spec Section 8 sets a target of "误差 < 5%".

**Impact**: Token budget will be frequently exceeded or under-utilized. Chinese users especially will hit budget limits prematurely.

**Recommendation**:
- Add a TODO comment referencing the spec target.
- Integrate tiktoken or jtokkit for accurate estimation. At minimum, document the known inaccuracy.

```java
// TODO: Replace with tiktoken/jtokkit for < 5% accuracy (spec NFR-8)
private long estimateTokens(String text) {
```

---

### LOW-2: `AgentExecutionAsyncConfig` thread pool parameters are hardcoded — not configurable

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/config/AgentExecutionAsyncConfig.java`
**Line**: 18-25

**Issue**: Core pool size (10), max pool size (50), queue capacity (200), and await termination (60s) are all hardcoded. The design.md Section 8.1 shows these as YAML-configurable values.

**Impact**: Cannot tune thread pool without code changes and redeployment.

**Recommendation**:
- Use `@Value` or `@ConfigurationProperties`:

```java
@Value("${agent.execution.async.core-pool-size:10}")
private int corePoolSize;
```

---

### LOW-3: `OpenAiProvider` and `AnthropicProvider` are stubs with no real implementation

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/model/OpenAiProvider.java`, `AnthropicProvider.java`

**Issue**: Both providers return empty strings. This is acknowledged as scaffolding, but there is no TODO or tracking issue for the real LangChain4j integration.

**Impact**: Week 2/3 integration work may be forgotten. The `generate()` methods are silently no-ops.

**Recommendation**:
- Add `// TODO(Week 2): Integrate LangChain4j OpenAiChatModel` comments.
- Consider throwing `UnsupportedOperationException` instead of returning empty strings to make the stub nature explicit.

---

### LOW-4: `AgentExecutionEngine.startExecution()` does not validate inputs

**File**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/AgentExecutionEngine.java`
**Line**: 28-38

**Issue**: `agentId`, `tenantId`, and `prompt` are not validated before use. A null `tenantId` would be persisted to the database. A null `agentId` could cause downstream NPEs.

**Recommendation**:
- Add precondition checks:

```java
public SfAgentExecution startExecution(Long agentId, String tenantId, String prompt) {
    if (agentId == null || tenantId == null || tenantId.isBlank() || prompt == null) {
        throw new IllegalArgumentException("agentId, tenantId, and prompt are required");
    }
```

---

### LOW-5: `AiModelRouterTest` uses `lenient = true` on all mocks — may hide real issues

**File**: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/model/AiModelRouterTest.java`
**Line**: 21-31

**Issue**: All mocks are marked `lenient = true`, which suppresses UnnecessaryStubbingException. While convenient, it means the test won't catch stubs that are never used, which can indicate test drift or missing assertions.

**Recommendation**:
- Remove `lenient = true` and use `lenient()` only for stubs that are genuinely optional across tests (like `redisTemplate.opsForValue()` in `@BeforeEach`).

---

## Spec/Design Alignment Checklist

| Requirement | Source | Status | Notes |
|-------------|--------|--------|-------|
| @Async + custom thread pool | design.md 5.1 | OK | Implemented with CallerRunsPolicy |
| QUEUED state | spec.md 5.1 | OK | Added to enum, used in Engine |
| State machine 11 states | spec.md 5.1 | PARTIAL | GATE_BLOCKED not in isTerminal() |
| ThinkingStateHandler LLM integration | spec.md 6 | OK | Full fallback chain, token budget |
| Token budget pre-check + post-deduction | design.md 6 | OK | Both input and output checked |
| AiModelRouter fallback | spec.md 6 | OK | generateWithFallback + generateWithMessages |
| Provider cooldown | design.md 8.1 | OK | 5 min Redis TTL |
| Configurable thread pool | design.md 8.1 | MISSING | Hardcoded values |
| Memory compression | design.md 7 | NOT IN SCOPE | Week 3 task |
| Loop detection | spec.md 6 | NOT IN SCOPE | Week 2 task |
| RetryingStateHandler | spec.md 6 | MISSING | File does not exist |
| ToolCallingStateHandler | spec.md 6 | STUB | parseToolCalls returns empty list |
| SSE event stream | spec.md 3.2 | NOT IN SCOPE | Week 4 task |
| Token budget JSON format | spec.md 4.2 | PARTIAL | Still comma-separated, spec wants structured JSON |
| Admission service | design.md 2.2 | STUB | Thresholds hardcoded |

---

## Test Coverage Assessment

| Test Class | Tests | Coverage Target | Notes |
|------------|-------|-----------------|-------|
| `AgentExecutionAsyncConfigTest` | 6 | Config params | Does not test actual async behavior |
| `AgentExecutionEngineTest` | 7 | State setting, delegation | Does not test `@Async` proxy |
| `ThinkingStateHandlerTest` | 14 | Handler logic | Good coverage of happy path and edge cases |
| `AiModelRouterTest` | 10 | Routing, fallback, messages | Missing stale cache test |
| `OpenAiProviderTest` | 4 | Stub verification | Trivial, will be replaced in Week 2 |
| `AnthropicProviderTest` | 4 | Stub verification | Trivial, will be replaced in Week 2 |
| **Total** | **45** | - | Good for Week 1 scaffolding |

**Coverage gaps**:
1. No test for `AgentExecutionState.isTerminal()` including `GATE_BLOCKED`.
2. No test for `containsToolCalls()` false-positive scenarios.
3. No integration test for actual `@Async` execution.
4. No test for `AgentRuntimeOrchestrator` + `AgentExecutionEngine` end-to-end.

---

## Security Checklist

| Check | Status | Notes |
|-------|--------|-------|
| No hardcoded secrets | PASS | API keys use env vars per design.md |
| Input validation | PARTIAL | Engine inputs not validated |
| SQL injection prevention | N/A | MyBatis-Plus, parameterized |
| Thread pool rejection policy | PASS | CallerRunsPolicy prevents silent drops |
| Resource leaks | PASS | Executor has graceful shutdown |
| Error message safety | PASS | No sensitive data in error messages |

---

## Action Items (Prioritized)

### Before Week 2 Merge
1. [HIGH-1] Fix `AgentExecutionEngineTest` async test or rename to reflect actual behavior.
2. [HIGH-2] Add full-context integration test for `AgentExecutionAsyncConfig`.
3. [MEDIUM-4] Fix `AgentExecutionState.isTerminal()` to include `GATE_BLOCKED`.

### Week 2 Iteration
4. [MEDIUM-1] Fix or remove `AiModelRouter` provider cache stale data issue.
5. [MEDIUM-2] Add specific exception handling in `ThinkingStateHandler`.
6. [MEDIUM-3] Replace substring-based tool call detection with structured parsing.
7. [LOW-2] Make thread pool parameters configurable via `@Value`.
8. [LOW-4] Add input validation to `AgentExecutionEngine.startExecution()`.

### Week 3+ / Backlog
9. [LOW-1] Integrate accurate token estimation (tiktoken/jtokkit).
10. [LOW-3] Replace provider stubs with real LangChain4j integration.
11. [LOW-5] Remove unnecessary `lenient = true` from mocks.
12. Add `RetryingStateHandler` (spec requirement, currently missing).
13. Migrate `token_budget_json` from comma-separated to structured JSON.

---

*Review completed. No CRITICAL issues found. 2 HIGH issues require attention before merge.*
