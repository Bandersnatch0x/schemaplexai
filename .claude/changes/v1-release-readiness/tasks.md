---
change_id: v1-release-readiness
status: in_progress
created_at: 2026-05-05
---

# Tasks: v1.0 Release Readiness

## Task Dependency Graph

```mermaid
graph TD
    T1[Task 1: Base Module Tests] --> T5[Task 5: Verify All Tests]
    T2[Task 2: Agent Engine Tests] --> T5
    T3[Task 3: Gateway/System Tests] --> T5
    T4[Task 4: P0 Issue Fixes] --> T5
    T5 --> T6[Task 6: Delivery Report]
```

## Parallel Execution Groups

- **Group A** (parallel): T1, T2, T3, T4

## Tasks

### Task 1: Base Module Tests
- **ID**: T1
- **Files**: `schemaplexai-common/src/test/java/**`, `schemaplexai-model/src/test/java/**`, `schemaplexai-dao/src/test/java/**`
- **Type**: test
- **Description**: Write unit tests for common, model, and dao modules
- **Acceptance Criteria**:
  - [ ] Result, ResultCode, BaseException tested
  - [ ] PageParam tested
  - [ ] TenantContextHolder tested
  - [ ] BaseEntity tested
  - [ ] All tests pass with `mvn test -pl schemaplexai-common,schemaplexai-model,schemaplexai-dao`
- **Estimated Hours**: 3h
- **Dependencies**: None
- **Status**: pending

### Task 2: Agent Engine Tests
- **ID**: T2
- **Files**: `schemaplexai-agent-engine/src/test/java/**`
- **Type**: test
- **Description**: Write comprehensive tests for agent-engine core components
- **Acceptance Criteria**:
  - [ ] ToolRegistry tested (register, resolve, parse, whitelist)
  - [ ] ToolSafetyGuard tested (all 4 dimensions)
  - [ ] TokenBudget tested (consume, exceed, remaining)
  - [ ] AgentLoopDetectionService tested (hash loop, tool sequence)
  - [ ] SecurityPolicyLoader tested (cache, default policy)
  - [ ] State machine transitions tested
  - [ ] All tests pass with `mvn test -pl schemaplexai-agent-engine`
- **Estimated Hours**: 4h
- **Dependencies**: None
- **Status**: pending

### Task 3: Gateway/System Tests
- **ID**: T3
- **Files**: `schemaplexai-gateway/src/test/java/**`, `schemaplexai-system/src/test/java/**`
- **Type**: test
- **Description**: Write tests for gateway filters and system services
- **Acceptance Criteria**:
  - [ ] JWT filter tested
  - [ ] Tenant filter tested
  - [ ] Rate limiter tested
  - [ ] Auth service tested
  - [ ] All tests pass with `mvn test -pl schemaplexai-gateway,schemaplexai-system`
- **Estimated Hours**: 3h
- **Dependencies**: None
- **Status**: pending

### Task 4: P0 Issue Fixes
- **ID**: T4
- **Files**: Various per CODE_REVIEW_REPORT.md
- **Type**: fix
- **Description**: Fix all P0 blocking issues from code review
- **Acceptance Criteria**:
  - [ ] All P0 issues resolved
  - [ ] `mvn clean compile` passes
  - [ ] No regression in existing functionality
- **Estimated Hours**: 4h
- **Dependencies**: None
- **Status**: pending

### Task 5: Verify All Tests
- **ID**: T5
- **Files**: All modules
- **Type**: verify
- **Description**: Run full test suite and verify coverage
- **Acceptance Criteria**:
  - [ ] `mvn clean test` passes (0 failures)
  - [ ] Coverage >= 80% for core modules
- **Estimated Hours**: 1h
- **Dependencies**: T1, T2, T3, T4
- **Status**: pending

### Task 6: Delivery Report
- **ID**: T6
- **Files**: `.claude/changes/v1-release-readiness/delivery-report.md`
- **Type**: docs
- **Description**: Write delivery report with test results and coverage
- **Acceptance Criteria**:
  - [ ] delivery-report.md written
  - [ ] Test pass/fail counts documented
  - [ ] Coverage percentages documented
- **Estimated Hours**: 0.5h
- **Dependencies**: T5
- **Status**: pending
