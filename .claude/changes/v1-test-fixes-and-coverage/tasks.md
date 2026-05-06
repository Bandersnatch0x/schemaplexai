---
change_id: v1-test-fixes-and-coverage
status: in_progress
created_at: 2026-05-05
---

# Tasks: Fix Pre-existing Test Failures + Add Coverage

## Task Dependency Graph

```mermaid
graph TD
    T1[Task 1: Source Fixes] --> T3[Task 3: Verify All Tests]
    T2[Task 2: Test Fixes] --> T3
    T3 --> T4[Task 4: JaCoCo Coverage]
    T4 --> T5[Task 5: Delivery Report]
```

## Parallel Execution Groups

- **Group A** (parallel): T1, T2

## Tasks

### Task 1: Source Fixes (4 files)
- **ID**: T1
- **Files**:
  - `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/extractor/FinalAnswerExtractor.java`
  - `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ExceptionHandlingStateHandler.java`
  - `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/ContainerToolSandbox.java`
- **Type**: fix
- **Description**: Fix regex pattern, immutable list, and validation ordering
- **Acceptance Criteria**:
  - [ ] THOUGHT_PATTERN lookahead includes `Thought`
  - [ ] ExceptionHandlingStateHandler uses mutable list
  - [ ] ContainerToolSandbox validates before whitelist check
- **Dependencies**: None
- **Status**: pending

### Task 2: Test Fixes (5 files)
- **ID**: T2
- **Files**:
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/state/ThinkingStateHandlerTest.java`
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/state/ToolCallingStateHandlerTest.java`
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/state/ExceptionHandlingStateHandlerTest.java`
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/memory/MemoryStrategyTest.java`
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/observability/ObservabilityRecorderTest.java`
  - `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestratorIntegrationTest.java`
- **Type**: fix
- **Description**: Add missing mocks, stub getCurrentState, adjust expectations, fix Spring context
- **Acceptance Criteria**:
  - [ ] All @Mock declarations added
  - [ ] getCurrentState stubbed in ExceptionHandlingStateHandlerTest
  - [ ] MemoryStrategyTest expectations match content-length estimation
  - [ ] Spring context exclusions added for integration tests
- **Dependencies**: None
- **Status**: pending

### Task 3: Verify All Tests
- **ID**: T3
- **Files**: All modules
- **Type**: verify
- **Description**: Run full test suite, verify 0 failures
- **Acceptance Criteria**:
  - [ ] `mvn clean test` passes with 0 failures
  - [ ] No regression in passing tests
- **Dependencies**: T1, T2
- **Status**: pending

### Task 4: JaCoCo Coverage
- **ID**: T4
- **Files**: `pom.xml` (parent)
- **Type**: feature
- **Description**: Add JaCoCo Maven plugin for coverage measurement
- **Acceptance Criteria**:
  - [ ] JaCoCo plugin configured in parent pom
  - [ ] `mvn verify` generates coverage reports
- **Dependencies**: T3
- **Status**: pending

### Task 5: Delivery Report
- **ID**: T5
- **Files**: `.claude/changes/v1-test-fixes-and-coverage/delivery-report.md`
- **Type**: docs
- **Description**: Write delivery report with test results and coverage
- **Dependencies**: T4
- **Status**: pending
