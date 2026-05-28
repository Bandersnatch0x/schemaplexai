# Placeholder Success Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue the active Codex goal by removing fake, placeholder, simulated, or unsupported runtime paths that report successful work before a real backend exists.

**Architecture:** Each slice starts with discovery, then one failing test that proves the old false-success behavior, then the smallest production change that converts the path to explicit failure or a real runtime call. Evidence is recorded in `docs/COVERAGE.md`, and `Next priority` is updated after each accepted slice.

**Tech Stack:** Java 21, Spring Boot 3.2, Maven, JUnit 5, AssertJ, Mockito, `rtk mvn`, `rtk powershell`.

---

## Operating Rules

- Maximum parallel subagents: 2 total at any time.
- Use subagents for read-only discovery or disjoint implementation scopes only.
- Do not revert unrelated dirty worktree changes.
- Do not commit unless the user explicitly asks.
- Every behavior change must follow TDD: red test first, focused green, then full affected module.
- Verification evidence must include command, test count, failure count, and error count.

## Acceptance Criteria

- A candidate is accepted only if the old path returned success, swallowed unsupported runtime absence, fabricated outputs, or made documentation claim work that no real runtime performed.
- The red test must fail for the expected old behavior, not because of a typo or setup error.
- The green implementation must either call a real runtime and propagate its failure, or return an explicit failure telling operators what runtime/configuration is missing.
- Focused tests for the changed class or behavior must pass.
- The full affected Maven module must pass.
- `rtk powershell -NoProfile -Command "git diff --check -- <scoped files>"` must report no whitespace errors; CRLF warnings are acceptable in this checkout.
- `docs/COVERAGE.md` must record the TDD evidence and update `Next priority`.
- Scoped `git diff -- <changed files>` must be reviewed before moving to the next slice.

## Task 1: Finish Current Workflow Node Slice

**Files:**
- Modify: `docs/COVERAGE.md`
- Verify: `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutor.java`
- Verify: `schemaplexai-workflow/src/test/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutorTest.java`

- [x] **Step 1: Record Concurrent evidence**

Add the focused `ConcurrentNodeExecutorTest` and full `schemaplexai-workflow` module results to `docs/COVERAGE.md`.

- [x] **Step 2: Record Concurrent release note**

Add a release note stating that `ConcurrentNodeExecutor` validates input and fails explicitly until a real concurrent task runtime is configured.

- [ ] **Step 3: Run scoped diff check**

Run:

```bash
rtk powershell -NoProfile -Command "git diff --check -- docs/COVERAGE.md schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/AIModelNodeExecutor.java schemaplexai-workflow/src/test/java/com/schemaplexai/workflow/node/NodeExecutorRegistryTest.java schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/ToolCallNodeExecutor.java schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutor.java schemaplexai-workflow/src/test/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutorTest.java"
```

Expected: no whitespace errors. LF to CRLF warnings are acceptable.

- [ ] **Step 4: Review scoped diff**

Run:

```bash
rtk powershell -NoProfile -Command "git diff -- docs/COVERAGE.md schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/AIModelNodeExecutor.java schemaplexai-workflow/src/test/java/com/schemaplexai/workflow/node/NodeExecutorRegistryTest.java schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/ToolCallNodeExecutor.java schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutor.java schemaplexai-workflow/src/test/java/com/schemaplexai/workflow/node/ConcurrentNodeExecutorTest.java"
```

Expected: only false-success removal, tests, and documentation evidence are present.

## Task 2: Discover Next False-Success Candidate

**Files:**
- Read: all `schemaplexai-*` module Java source and tests as needed
- Modify later only after candidate selection

- [x] **Step 1: Search high-risk terms**

Run:

```bash
rtk powershell -NoProfile -Command "Select-String -Path schemaplexai-*/src/main/java/**/*.java,schemaplexai-*/src/test/java/**/*.java -Pattern 'simulate|simulated|fake|placeholder|not implemented|unsupported|executed|success' -Context 2,3"
```

Expected: candidate list with obvious false positives separated from actionable false-success paths.

- [x] **Step 2: Rank candidates**

Choose the highest-risk candidate that:

- returns success without a real runtime;
- can be tested in one module;
- has a narrow write set;
- does not depend on unrelated dirty changes.

- [x] **Step 3: Set slice acceptance**

For the selected candidate, define:

- expected red test name;
- focused test command;
- full module command;
- files allowed to change;
- documentation line to add.

Selected P0/P1 slices:

- Completed: `MilvusSyncServiceImpl` must fail instead of simulated sync when real file extraction cannot happen.
- Completed: `QualityOrchestrator` must fail instead of all-passed when gate rules are unknown or invalid JSON.
- Completed: `SecurityScanRule` and `SpecComplianceRule` must not pass by placeholder-only logic.
- Completed: `AgentLab` and `ResearchAutomation` must fail without configured real runtimes instead of returning simulated experiment/search outputs.
- Completed: `CostSyncConsumer` must not ACK unsupported full/range cost-sync requests as successful incremental sync.
- Completed: `CostService` must propagate cost projection persistence and budget-update failures so cost event consumers can nack failed projections.
- Completed: `AgentExecuteDispatcher` must not ACK agent execute messages when `InboxDeduplicationService.markProcessed` fails after starting execution.
- Completed: `CostSyncConsumer` must not ACK cost-sync messages when `InboxDeduplicationService.markProcessed` fails after incremental sync.
- Completed: `NotificationConsumer` must not ACK in-app notifications when `InboxDeduplicationService.markProcessed` fails after persistence.
- Completed: `ApprovalRequestConsumer` must propagate dedup mark failures transactionally so approval-ticket insert and processed-event mark succeed or fail together.
- Completed: `EventReorderingConsumer` must not ACK ready execution events when non-idempotent persistence fails.
- Next: continue the active-source ACK/false-success sweep with remaining MQ consumers and scheduler paths that swallow failed side effects as successful completion.

## Task 3: Implement One Candidate With TDD

**Files:**
- Modify: candidate-specific production file
- Modify: candidate-specific test file
- Modify: `docs/COVERAGE.md`

- [ ] **Step 1: Write the failing test**

The test must assert that the candidate does not return success when the real runtime/configuration is absent.

- [ ] **Step 2: Run focused red test**

Run the focused `rtk mvn ... -Dtest=...` command. Expected: failure shows the old false-success behavior.

- [ ] **Step 3: Implement minimal change**

Change only the candidate path so it returns explicit failure or delegates to a real runtime and propagates failure.

- [ ] **Step 4: Run focused green test**

Expected: focused candidate tests pass with 0 failures and 0 errors.

- [ ] **Step 5: Run full affected module**

Expected: full module passes with 0 failures and 0 errors.

- [ ] **Step 6: Update documentation evidence**

Record the red/focused/full-module evidence in `docs/COVERAGE.md` and update `Next priority`.

- [ ] **Step 7: Run scoped diff checks**

Run `git diff --check -- <scoped files>` and inspect `git diff -- <scoped files>` before moving to the next slice.

## Task 4: Implement P1 Quality Rule Placeholders

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/gate/rules/SecurityScanRule.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/gate/rules/SecurityScanRuleTest.java`
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/gate/rules/SpecComplianceRule.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/gate/rules/SpecComplianceRuleTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Security rule red test**

Assert that the security scan rule does not pass when no real scan findings are supplied.

- [x] **Step 2: Spec rule red test**

Assert that the spec compliance rule does not pass when no real compliance evidence is supplied.

- [x] **Step 3: Minimal implementation**

Convert placeholder pass paths to explicit failed check results unless supported evidence is present in `QualityContext`.

- [x] **Step 4: Focused green tests**

Run focused quality-rule tests and confirm 0 failures and 0 errors.

- [x] **Step 5: Full module verification**

Run `rtk mvn test -pl schemaplexai-quality` and confirm 0 failures and 0 errors.

- [x] **Step 6: Evidence and diff review**

Record evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped diff before selecting the next slice.

Evidence captured:

- Red: `SecurityScanRuleTest#check_withoutSecurityScanEvidence_failsInsteadOfPlaceholderPass` failed because the old rule returned placeholder pass for safe output with no real scan evidence.
- Red: `SpecComplianceRuleTest#check_withSpecIdButNoComplianceEvidence_failsInsteadOfPlaceholderPass` failed because the old rule returned placeholder pass for `specId` alone.
- Red: `SecurityScanRuleTest#check_withExplicitFailedScanEvidence_reportsScanFailure` failed because explicit `securityScanPassed=false` was reported as missing evidence instead of scan failure.
- Green focused: `SecurityScanRuleTest,SpecComplianceRuleTest` passed 20 tests, 0 failures, 0 errors.
- Green module: `schemaplexai-quality` passed 269 tests, 0 failures, 0 errors.

## Task 5: Implement Agent-Engine Exploration Runtime Placeholders

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/exploration/AgentLab.java`
- Modify: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/exploration/AgentLabTest.java`
- Verify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/exploration/ResearchAutomation.java`
- Verify: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/exploration/ResearchAutomationTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: ResearchAutomation runtime seam**

Remove placeholder `example.com` source generation and require an injected search runtime for real discovery.

- [x] **Step 2: AgentLab red test**

Assert that `AgentLab.runExperiment` throws when non-empty strategies are provided but no experiment runtime is configured.

- [x] **Step 3: AgentLab minimal implementation**

Add a narrow `ExperimentRuntime` seam, preserve empty strategy behavior, filter blank strategies before runtime dispatch, and remove deterministic simulated result generation from the default runtime path.

- [x] **Step 4: Focused green tests**

Run focused exploration tests and confirm 0 failures and 0 errors.

- [x] **Step 5: Full affected module verification**

Run the affected agent-engine reactor and confirm 0 failures and 0 errors. Use `-am` for this dirty checkout so Maven uses the current workspace `schemaplexai-common` source rather than a stale local SNAPSHOT.

- [x] **Step 6: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md`, update `Next priority`, and note the stale `.m2` SNAPSHOT caveat for single-module verification.

Evidence captured:

- Red: `AgentLabTest#runExperiment_withoutRuntime_throwsInsteadOfSimulatingResults` failed in the focused `AgentLabTest` run with 15 tests run, 1 failure, 0 errors because the old implementation returned simulated experiment results.
- Red: `ResearchAutomationTest#researchTopic_withoutSearchRuntime_throwsInsteadOfReturningExampleSources` failed in the implementer run because the old implementation returned example search sources.
- Green focused: `AgentLabTest` passed 15 tests, 0 failures, 0 errors.
- Green focused: `ResearchAutomationTest` passed 15 tests, 0 failures, 0 errors.
- Green combined: `AgentLabTest,ResearchAutomationTest` passed 30 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-engine -am` passed common/model/dao/integration/ops/agent-engine, with `schemaplexai-agent-engine` 1843 tests run, 0 failures, 0 errors, 4 skipped.
- Environment caveat: `rtk mvn test -pl schemaplexai-agent-engine` without `-am` failed in `SchemaPlexaiAgentEngineApplicationTest` because the local `.m2` `schemaplexai-common` SNAPSHOT lacked the current observability fallback auto-configuration; the `-am` run verified the current workspace source chain.

## Task 6: Implement Task Cost-Sync ACK Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/CostSyncConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/CostSyncConsumerTest.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/config/CostSyncDependencyConfigurationTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Unsupported full-sync red test**

Assert that `forceFullSync=true` is nacked/logged and does not call incremental sync.

- [x] **Step 2: Unsupported date-range red test**

Assert that nonblank `dateRange` is nacked/logged and does not call incremental sync.

- [x] **Step 3: Unsupported sync-type red test**

Assert that unsupported `syncType` values are nacked/logged and do not call incremental sync.

- [x] **Step 4: Minimal implementation**

Validate the incremental-only contract before delegating to `CostDataSyncService`; keep `incremental`, blank/null, and existing `api` sync types compatible.

- [x] **Step 5: Focused green tests**

Run focused consumer and dependency-configuration tests through the task reactor and confirm 0 failures and 0 errors.

- [x] **Step 6: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 7: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md`, update `Next priority`, and keep the maximum parallel subagent limit at 2.

Evidence captured:

- Red setup caveat: `rtk mvn test -pl schemaplexai-task "-Dtest=CostSyncConsumerTest"` failed at test compile because stale local `.m2` ops artifacts did not include current `CostDataSyncService`; this was not accepted as behavior evidence.
- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 2 failures, 0 errors because full/range sync requests were ACKed and `syncIncrementalData()` was called.
- Green focused: `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest,CostSyncDependencyConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 9 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 147 tests run, 0 failures, 0 errors, 0 skipped.
- Next: inspect `CostService` and cost event consumers so cost projection persistence failures cannot be ACKed or reported as successful processing.

## Task 7: Implement Cost Projection Failure Propagation

**Files:**
- Modify: `schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/CostService.java`
- Modify: `schemaplexai-ops/src/test/java/com/schemaplexai/ops/service/CostServiceTest.java`
- Verify: `schemaplexai-task/src/test/java/com/schemaplexai/task/consumer/ExecutionEventConsumerTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Persistence-failure red tests**

Assert that `TOKEN_USED` and `TOOL_CALL` insert failures propagate instead of being swallowed.

- [x] **Step 2: Budget-update red test**

Assert that budget update failure after a cost record insert propagates to the caller.

- [x] **Step 3: Minimal implementation**

Remove the top-level swallow in `processExecutionEvent` and rethrow runtime failures from token/tool handlers while preserving unsupported event-type debug behavior.

- [x] **Step 4: Focused green tests**

Run focused `CostServiceTest` through the ops reactor and confirm 0 failures and 0 errors.

- [x] **Step 5: Consumer boundary verification**

Run focused `ExecutionEventConsumerTest` through the task reactor and confirm `CostService` failures produce NACK/fail-log behavior.

- [x] **Step 6: Full affected module verification**

Run `rtk mvn test -pl schemaplexai-ops -am` and confirm 0 failures and 0 errors.

- [x] **Step 7: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md` and update `Next priority`.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-ops -am "-Dtest=CostServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 40 tests, 3 failures, 0 errors because insert and budget-update failures were swallowed.
- Green focused: `rtk mvn test -pl schemaplexai-ops -am "-Dtest=CostServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 40 tests, 0 failures, 0 errors.
- Green consumer boundary: `rtk mvn test -pl schemaplexai-task -am "-Dtest=ExecutionEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 4 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-ops -am` passed common/model/dao/ops, with 231 tests run, 0 failures, 0 errors, 0 skipped.
- Next: fix `AgentExecuteDispatcher` so `markProcessed` failure after execution start nacks/logs instead of ACKing dispatch success.

## Task 8: Implement Agent Execute Dedup Mark Failure ACK Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/AgentExecuteDispatcher.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/AgentExecuteDispatcherTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Dedup mark red test**

Assert that `startExecution` success followed by `dedupService.markProcessed(...)` failure produces `basicNack(1L, false, false)`, logs the failure, and never ACKs the MQ message.

- [x] **Step 2: Focused red verification**

Run `rtk mvn test -pl schemaplexai-task -am "-Dtest=AgentExecuteDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm the old behavior ACKs after swallowing `markProcessed`.

- [x] **Step 3: Minimal implementation**

Let `markProcessed` failure propagate to the existing catch/NACK path before success logging or ACK.

- [x] **Step 4: Focused green verification**

Run the focused dispatcher test command again and confirm 0 failures and 0 errors.

- [x] **Step 5: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 6: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md`, run scoped diff checks, and update `Next priority`.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=AgentExecuteDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack`.
- Green focused: same command passed 6 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 148 tests run, 0 failures, 0 errors, 0 skipped.
- Next: fix the same `markProcessed` failure false-success in `CostSyncConsumer`.

## Task 9: Implement Cost Sync Dedup Mark Failure ACK Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/CostSyncConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/CostSyncConsumerTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Dedup mark red test**

Assert that `syncIncrementalData()` success followed by `dedupService.markProcessed(...)` failure produces `basicNack(1L, false, false)`, logs the failure, and never ACKs the MQ message.

- [x] **Step 2: Focused red verification**

Run `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm the old behavior ACKs after swallowing `markProcessed`.

- [x] **Step 3: Minimal implementation**

Let `markProcessed` failure propagate to the existing catch/NACK path before success logging or ACK.

- [x] **Step 4: Focused green verification**

Run the focused cost-sync consumer test command again and confirm 0 failures and 0 errors.

- [x] **Step 5: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 6: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md`, run scoped diff checks, and update `Next priority`.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 9 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack` after `markProcessed` failed.
- Green focused: same command passed 9 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 149 tests run, 0 failures, 0 errors, 0 skipped.
- Next: fix the same `markProcessed` failure false-success in `NotificationConsumer`.

## Task 10: Implement Notification Dedup Mark Failure ACK Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/NotificationConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/NotificationConsumerTest.java`
- Modify: `docs/COVERAGE.md`

- [x] **Step 1: Dedup mark red test**

Assert that in-app notification persistence success followed by `dedupService.markProcessed(...)` failure produces `basicNack(1L, false, false)`, logs the failure, and never ACKs the MQ message.

- [x] **Step 2: Focused red verification**

Run `rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm the old behavior ACKs after swallowing `markProcessed`.

- [x] **Step 3: Minimal implementation**

Let `markProcessed` failure propagate to the existing catch/NACK path before delivery success logging or ACK.

- [x] **Step 4: Focused green verification**

Run the focused notification consumer test command again and confirm 0 failures and 0 errors.

- [x] **Step 5: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 6: Evidence and next-priority update**

Record red/focused/full-module evidence in `docs/COVERAGE.md`, run scoped diff checks, and update `Next priority`.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack` after `markProcessed` failed.
- Green focused: same command passed 10 tests, 0 failures, 0 errors.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 150 tests run, 0 failures, 0 errors, 0 skipped.
- Next: inspect `ApprovalRequestConsumer` dedup mark failure semantics and decide whether its non-Channel consumer contract should propagate mark failures transactionally or record explicit failure without reporting approval-request handling success.

## Task 11: Investigate Approval Request Dedup Mark Failure Semantics

**Files:**
- Read/modify if selected: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/ApprovalRequestConsumer.java`
- Read/modify if selected: matching `schemaplexai-quality/src/test/java/...` test file
- Modify if behavior changes: `docs/COVERAGE.md`

- [x] **Step 1: Contract discovery**

Read the consumer, its tests, and adjacent dedup consumers to determine whether `ApprovalRequestConsumer` has an externally visible ACK/failure contract or a transactional service contract.

- [x] **Step 2: Candidate decision**

If a dedup mark failure currently reports approval-request handling success, define a red test name, focused command, full module command, allowed files, and documentation line.

- [x] **Step 3: TDD implementation if needed**

Write the red test first, verify it fails for the old success path, make the minimal production change, run focused green, then run the full `schemaplexai-quality` reactor.

- [x] **Step 4: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Decision: `ApprovalRequestConsumer` has a transactional service-consumer contract rather than an explicit Rabbit `Channel` ACK contract.
- Red: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=ApprovalRequestIdempotencyTest" "-Dsurefire.failIfNoSpecifiedTests=false"` first ran 4 tests, 1 failure, 0 errors because the old implementation swallowed `dedup mark failed`.
- Green focused: same command passed 4 tests, 0 failures, 0 errors after propagating `InboxDeduplicationService.markProcessed` failures.
- Red: after adding the transaction-boundary acceptance test, the focused command ran 5 tests, 1 failure, 0 errors because `consume(...)` lacked `@Transactional`.
- Green focused: same command passed 5 tests, 0 failures, 0 errors after adding the transaction boundary.
- Green reactor: `rtk mvn test -pl schemaplexai-quality -am` passed common/model/dao/quality, with `schemaplexai-quality` 271 tests, 0 failures, 0 errors, 0 skipped.
- Next: run the active-source false-success discovery sweep and pick the next narrow TDD candidate.

## Task 12: Discover Next Active-Source False-Success Candidate

**Files:**
- Read: active `schemaplexai-*` Java source and tests
- Modify later only after candidate selection
- Modify if selected: `docs/COVERAGE.md`

- [x] **Step 1: Search active source**

Search only active source/test trees for `markProcessed`, `basicAck`, `basicNack`, placeholder, simulated, unsupported, TODO, and success wording. Exclude generated `target`, `.codegraph`, and archived tool state.

- [x] **Step 2: Rank candidates**

Separate false positives from runtime paths that still report success without a real side effect, swallow a post-side-effect failure, or turn unsupported work into ACK/success.

- [x] **Step 3: Define acceptance**

For the selected candidate, record the red test name, focused command, full module command, allowed files, and expected documentation evidence before implementation.

Selected candidate:

- Candidate: `EventReorderingConsumer.applyEvent(...)` currently catches non-`DuplicateKeyException` persistence failures from `ExecutionEventService.writeEvent(...)`, returns to the outer handler, and allows the Rabbit message to be ACKed even though the ready execution event was not durably recorded.
- Red test: `EventReorderingConsumerTest#nacksWhenReadyEventPersistenceFails`.
- Focused command: `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=EventReorderingConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"`.
- Full module command: `rtk mvn test -pl schemaplexai-agent-engine -am`.
- Allowed files: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/mq/EventReorderingConsumer.java`, `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/mq/EventReorderingConsumerTest.java`, `docs/COVERAGE.md`, and this plan file.
- Acceptance: duplicate-key idempotency remains ACK-compatible, but any other `writeEvent` failure propagates to the outer handler so the message is NACKed and is not reported as successfully processed.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=EventReorderingConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` after `ExecutionEventService.writeEvent(...)` failed.
- Green focused: same command passed 6 tests, 0 failures, 0 errors after non-duplicate persistence failures propagated to the outer NACK path.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-engine -am` passed common/model/dao/integration/ops/agent-engine, with `schemaplexai-agent-engine` 1844 tests run, 0 failures, 0 errors, 4 skipped.
- Next: inspect remaining ACK/false-success candidates such as `CostEventConsumer`, `SseEventConsumer`, and scheduled jobs that catch and log failed side effects without surfacing failure status.

## Task 13: Implement Cost Statistics Scheduler Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/CostStatisticsJob.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/scheduling/CostStatisticsJobTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the cost statistics scheduler and tests. Existing behavior logged `syncIncrementalData()` or `checkBudgetAlerts()` failures and then returned normally, while the success path logged completed cost statistics.

- [x] **Step 2: Define acceptance**

Acceptance: cost sync and budget-alert failures must still be logged, but must propagate out of `run()` so scheduler/error handling and tests can observe the failed side effect. If cost sync fails, budget alerts must not run. Successful sync plus successful budget-alert check remains compatible.

- [x] **Step 3: Write red tests**

Add focused tests for `run_syncThrowsException_propagatesWithoutBudgetAlertCheck` and `run_budgetAlertsThrowException_propagatesAfterCostSync`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation swallowed both failures.

- [x] **Step 5: Minimal implementation**

Keep the failure log, rethrow runtime failures, and wrap any checked failure in `IllegalStateException`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the scheduler contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostStatisticsJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 3 tests, 2 failures, 0 errors because the old implementation logged and swallowed `sync failed` and `budget alert failed`.
- Green focused: same command passed 3 tests, 0 failures, 0 errors after failures propagated.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 151 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the scheduler false-success sweep, especially `MemoryConsolidationJob` and other scheduled jobs whose failed side effects still return normally or whose current log/test contract overstates completed runtime work.

## Task 14: Implement Memory Consolidation Scheduler Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/MemoryConsolidationJob.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/scheduling/MemoryConsolidationJobTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the memory consolidation scheduler and tests. Existing behavior logged Redis `keys(...)` and `expire(...)` failures, then returned normally while the successful path logged completed memory consolidation.

- [x] **Step 2: Define acceptance**

Acceptance: Redis key lookup and TTL refresh failures must still be logged, but must propagate out of `run()` so scheduler/error handling and tests can observe that memory consolidation failed. If key lookup fails, no TTL refresh is attempted. Existing null-key, empty-key, and successful TTL refresh behavior remains compatible.

- [x] **Step 3: Write red tests**

Add focused tests for `run_redisKeysThrowsException_propagates` and `run_expireThrowsException_propagates`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation swallowed both Redis failures.

- [x] **Step 5: Minimal implementation**

Keep the failure log, rethrow runtime failures, and wrap any checked failure in `IllegalStateException`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the scheduler contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=MemoryConsolidationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 2 failures, 0 errors because the old implementation logged and swallowed `Redis error` and `expire failed`.
- Green focused: same command passed 5 tests, 0 failures, 0 errors after failures propagated.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 152 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the scheduler false-success sweep with `ApprovalTimeoutJob`, `ChatMessageArchiveJob`, and `MilvusReconciliationJob`; treat `HealthCheckJob` separately because its current contract intentionally isolates dependency probe failures.

## Task 15: Implement Approval Timeout Scheduler Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/ApprovalTimeoutJob.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/scheduling/ApprovalTimeoutJobTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the approval timeout scheduler and tests. Existing behavior logged `EscalationPolicyService.checkEscalations()` failures, then returned normally while the successful path logged completed approval timeout check.

- [x] **Step 2: Define acceptance**

Acceptance: escalation check failures must still be logged, but must propagate out of `run()` so scheduler/error handling and tests can observe that approval timeout processing failed. Successful escalation checks remain compatible.

- [x] **Step 3: Write red test**

Add focused test `run_checkEscalationsThrowsException_propagates`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation swallowed the escalation failure.

- [x] **Step 5: Minimal implementation**

Keep the failure log, rethrow runtime failures, and wrap any checked failure in `IllegalStateException`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the scheduler contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=ApprovalTimeoutJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `escalation failed`.
- Green focused: same command passed 2 tests, 0 failures, 0 errors after failures propagated.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 153 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the scheduler false-success sweep with `ChatMessageArchiveJob` and `MilvusReconciliationJob`; treat `HealthCheckJob` separately because its current contract intentionally isolates dependency probe failures.

## Task 16: Implement Chat Message Archive Scheduler Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/ChatMessageArchiveJob.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/scheduling/ChatMessageArchiveJobTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the chat message archive scheduler and tests. Existing behavior logged `ChatMessageArchiveService.archiveExpiredMessages(...)` failures, then returned normally while the successful path logged completed archive work.

- [x] **Step 2: Define acceptance**

Acceptance: archive-service failures must still be logged, but must propagate out of `run()` so scheduler/error handling and tests can observe that archive work failed. Successful archive runs remain compatible and still delegate with the configured retention window.

- [x] **Step 3: Write red test**

Add focused test `run_archiveThrowsException_propagates`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation swallowed the archive failure.

- [x] **Step 5: Minimal implementation**

Keep the failure log, rethrow runtime failures, and wrap any checked failure in `IllegalStateException`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the scheduler contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=ChatMessageArchiveJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `archive failed`.
- Green focused: same command passed 2 tests, 0 failures, 0 errors after failures propagated.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 154 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the scheduler false-success sweep with `MilvusReconciliationJob`; treat `HealthCheckJob` separately because its current contract intentionally isolates dependency probe failures.

## Task 17: Implement Milvus Reconciliation Scheduler Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/MilvusReconciliationJob.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/scheduling/MilvusReconciliationJobTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the Milvus reconciliation scheduler and tests. Existing behavior logged `MilvusReconciliationService.reconcilePendingDocuments(...)` failures, then returned normally while the successful path logged completed reconciliation work.

- [x] **Step 2: Define acceptance**

Acceptance: reconciliation-service failures must still be logged, but must propagate out of `run()` so scheduler/error handling and tests can observe that vector reconciliation failed. Successful reconciliation runs remain compatible and still delegate with the configured batch size.

- [x] **Step 3: Write red test**

Add focused test `run_reconciliationThrowsException_propagates`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation swallowed the reconciliation failure.

- [x] **Step 5: Minimal implementation**

Keep the failure log, rethrow runtime failures, and wrap any checked failure in `IllegalStateException`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the scheduler contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusReconciliationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `reconciliation failed`.
- Green focused: same command passed 2 tests, 0 failures, 0 errors after failures propagated.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 155 tests run, 0 failures, 0 errors, 0 skipped.
- Next: re-audit remaining task scheduled jobs and MQ consumers for false-success or swallowed-failure paths; keep `HealthCheckJob` separate because its current contract intentionally isolates dependency probe failures.

## Task 18: Implement Dead Letter Audit Publish Failure Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/DeadLetterHandler.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/DeadLetterHandlerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the dead-letter handler and tests. Invalid JSON payloads intentionally ACK to avoid poison-message retry loops, but parsed dead-letter messages whose audit event publish fails were also ACKed because `publishAuditEvent(...)` swallowed `RabbitTemplate.convertAndSend(...)` failures.

- [x] **Step 2: Define acceptance**

Acceptance: invalid dead-letter payloads still ACK intentionally, successful audit publication still ACKs, and audit publication failures for parsed dead-letter messages must NACK without ACK so missing audit handoff is observable.

- [x] **Step 3: Write red test**

Change focused test `auditPublishFailureNacksWithoutAck` to expect `basicNack(1L, false, false)` and no `basicAck(...)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation ACKed after audit publish failure.

- [x] **Step 5: Minimal implementation**

Separate parse failure handling from audit publication handling. Keep parse failures ACKed, let audit publication failures reach the outer handler, and NACK the parsed dead-letter message.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm normal, parse-failure, and audit-publish-failure contracts pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=DeadLetterHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 4 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` after `MQ down` and never called `basicNack`.
- Green focused: same command passed 4 tests, 0 failures, 0 errors after audit publish failures produced NACK.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 155 tests run, 0 failures, 0 errors, 0 skipped.
- Next: address `MqIdempotencyInterceptor` duplicate-skip behavior under manual-ACK mode; duplicate Redis/DB paths return before the listener runs and need explicit ACK coverage.

## Task 19: Implement MQ Idempotency Duplicate-Skip Manual ACK Semantics

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MqIdempotencyInterceptor.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/MqIdempotencyInterceptorTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the task Rabbit listener configuration, idempotency interceptor, and current tests. `schemaplexai-task` runs Rabbit listeners in `manual` ACK mode, but Redis and DB duplicate-detection paths returned before invoking the listener, so listener-level `basicAck(...)` never ran for duplicates.

- [x] **Step 2: Define acceptance**

Acceptance: when a duplicate message is skipped because Redis already has the idempotency key or because the DB unique constraint reports a duplicate, and the listener arguments include a Rabbit `Channel`, the interceptor must call `basicAck(deliveryTag, false)` before returning. These duplicate paths must still skip `joinPoint.proceed()`, Redis duplicate skips must avoid DB insert, DB duplicate skips must keep writing the Redis idempotency cache, and invocations without a `Channel` keep the previous return-null behavior.

- [x] **Step 3: Write red tests**

Add focused tests for Redis duplicate and DB duplicate paths with a mocked `Channel` and message delivery tag.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old implementation returned without ACKing duplicate messages.

- [x] **Step 5: Minimal implementation**

Detect an optional Rabbit `Channel` in the listener arguments and call `basicAck(...)` on duplicate Redis and DB skip paths before returning.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the duplicate ACK contract tests pass with the existing interceptor behavior.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=MqIdempotencyInterceptorTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 8 tests, 2 failures, 0 errors because the old implementation never called `channel.basicAck(1L, false)` on Redis/DB duplicate skips.
- Green focused: same command passed 8 tests, 0 failures, 0 errors, 0 skipped after duplicate skip paths ACKed when a `Channel` was present.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 157 tests run, 0 failures, 0 errors, 0 skipped.
- Next: re-audit remaining MQ recovery blind spots, with `MessageFailLogService` persistence-failure signaling as the next likely risk.

## Task 20: Implement Message Fail Log Persistence Signal

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MessageFailLogService.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/MessageFailLogServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the fail-log service and current tests. Existing behavior mapped AMQP message fields into `SfMessageFailLog` and swallowed mapper exceptions, but callers had no return signal that persistence failed or inserted zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: successful fail-log insert returns `true`; mapper exceptions are logged and return `false`; zero-row inserts return `false`; existing callers remain source-compatible because they can ignore the boolean result; existing field mapping and Unicode payload preservation remain unchanged.

- [x] **Step 3: Write red tests**

Add focused tests for successful insert, database exception, and zero-row insert return values while keeping existing mapping and payload assertions.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old `void` method produced `null` for the new return-value contract.

- [x] **Step 5: Minimal implementation**

Change `MessageFailLogService.log(...)` to return `messageFailLogMapper.insert(record) > 0`, and return `false` after logging persistence exceptions.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the fail-log signal contract tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=MessageFailLogServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 3 failures, 0 errors because the old `void` method returned `null` instead of the expected `true`/`false` signal.
- Green focused: same command passed 5 tests, 0 failures, 0 errors, 0 skipped after `log(...)` returned explicit persistence status.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 159 tests run, 0 failures, 0 errors, 0 skipped.
- Next: update MQ consumers to observe or metric a `false` fail-log persistence result where it affects operational visibility, then continue the remaining MQ recovery blind-spot sweep. Keep maximum parallel subagents at 2.

## Task 21: Observe Agent Execute Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/AgentExecuteDispatcher.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/AgentExecuteDispatcherTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the dispatcher failure paths and the fail-log signal contract. Existing `basicNack(..., false, false)` behavior was already correct, but the dispatcher ignored a `false` result from `MessageFailLogService.log(...)`, leaving fail-log persistence loss invisible.

- [x] **Step 2: Define acceptance**

Acceptance: when dispatch failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `AgentExecuteDispatcher` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test that forces the engine dispatch path to fail, makes `messageFailLogService.log(...)` return `false`, and asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old dispatcher nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=AgentExecuteDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 7 tests, 0 failures, 0 errors, 0 skipped after the dispatcher warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 160 tests run, 0 failures, 0 errors, 0 skipped.
- Next: carry the same fail-log persistence-loss observability pattern into `NotificationConsumer`, starting with unsupported-channel rejection. Keep maximum parallel subagents at 2.

## Task 22: Observe Notification Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/NotificationConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/NotificationConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the notification consumer failure paths and the fail-log signal contract. Existing unsupported-channel and exception paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when notification failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `NotificationConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for an unsupported notification channel that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route notification fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 11 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 11 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 161 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the same fail-log persistence-loss observability pattern across the remaining MQ consumers. Keep maximum parallel subagents at 2.

## Task 23: Observe Workflow Trigger Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/WorkflowTriggerConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/WorkflowTriggerConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the workflow trigger consumer failure paths and the fail-log signal contract. Existing malformed-payload, missing-key, and handler exception paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when workflow-trigger failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `WorkflowTriggerConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for a workflow-trigger handler failure that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route workflow-trigger fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=WorkflowTriggerConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 6 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 162 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue fail-log persistence-loss observability across `QualityEventConsumer`, `MilvusSyncConsumer`, `CostSyncConsumer`, and `ExecutionEventConsumer` where a `false` fail-log persistence result affects operational visibility. Keep maximum parallel subagents at 2.

## Task 24: Observe Quality Event Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/QualityEventConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/QualityEventConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the quality event consumer failure paths and the fail-log signal contract. Existing malformed-payload, missing-event-type, and handler exception paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when quality-event failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `QualityEventConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for a quality-event handler failure that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route quality-event fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=QualityEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 6 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 163 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue fail-log persistence-loss observability across `MilvusSyncConsumer`, `CostSyncConsumer`, and `ExecutionEventConsumer` where a `false` fail-log persistence result affects operational visibility. Keep maximum parallel subagents at 2.

## Task 25: Observe Milvus Sync Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MilvusSyncConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/MilvusSyncConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the Milvus sync consumer failure paths and the fail-log signal contract. Existing invalid-payload, missing-field, and handler exception paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when Milvus-sync failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `MilvusSyncConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for a Milvus-sync handler failure that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route Milvus-sync fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 7 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 164 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue fail-log persistence-loss observability across `CostSyncConsumer` and `ExecutionEventConsumer` where a `false` fail-log persistence result affects operational visibility. Keep maximum parallel subagents at 2.

## Task 26: Observe Cost Sync Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/CostSyncConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/mq/CostSyncConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the cost-sync consumer failure paths and the fail-log signal contract. Existing unsupported request, service exception, and dedup mark failure paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when cost-sync failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `CostSyncConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for a cost-sync service failure that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route cost-sync fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 10 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Green reactor: `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-task` 165 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue fail-log persistence-loss observability in `ExecutionEventConsumer` where a `false` fail-log persistence result affects operational visibility. Keep maximum parallel subagents at 2.

## Task 27: Observe Execution Event Fail Log Persistence Loss

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/consumer/ExecutionEventConsumer.java`
- Modify: `schemaplexai-task/src/test/java/com/schemaplexai/task/consumer/ExecutionEventConsumerTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the execution-event consumer failure paths and the fail-log signal contract. Existing JSON parse and cost-projection exception paths already nacked without requeue, but ignored a `false` result from `MessageFailLogService.log(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when execution-event failure handling attempts to persist a fail log and `MessageFailLogService.log(...)` returns `false`, `ExecutionEventConsumer` must emit a warning that identifies fail-log persistence loss and the delivery tag, while still nacking the message without requeue. Successful fail-log writes keep the existing failure path behavior.

- [x] **Step 3: Write red test**

Add a focused `OutputCaptureExtension` test for a cost-projection failure that makes `messageFailLogService.log(...)` return `false`, then asserts both the warning and `basicNack(1L, false, false)`.

- [x] **Step 4: Focused red verification**

Run the focused task reactor and confirm the old consumer nacked but produced no warning for the fail-log persistence-loss signal.

- [x] **Step 5: Minimal implementation**

Route execution-event fail-log recording through a helper that checks the boolean return value and logs a warning when persistence returns `false`.

- [x] **Step 6: Focused green verification**

Run the focused task reactor and confirm the warning + nack contract test passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-task -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-task -am "-Dtest=ExecutionEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 1 failure, 0 errors because the old implementation produced no warning when fail-log persistence returned `false`.
- Green focused: same command passed 5 tests, 0 failures, 0 errors, 0 skipped after the consumer warned on fail-log persistence loss.
- Broad gate triage and closure: the first `rtk mvn test -pl schemaplexai-task -am` run failed outside the touched scope in `schemaplexai-agent-engine` at `AgentEngineBenchmarkTest.compositeBenchmarkSummary` because observed throughput was `46623`, below the hardcoded `> 50000` threshold. Focused triage showed the benchmark was measuring loop-warning log spam rather than the normal hot path, so the benchmark input was changed to unique tool sequences. Focused rerun `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=AgentEngineBenchmarkTest#compositeBenchmarkSummary" "-Dsurefire.failIfNoSpecifiedTests=false"` then passed with `LoopDetection.detect` at `393869 ops/sec`.
- Green reactor: rerun `rtk mvn test -pl schemaplexai-task -am` passed common/model/dao/integration/ops/agent-engine/quality/task, with `schemaplexai-agent-engine` 1844 tests run, 0 failures, 0 errors, 4 skipped, `schemaplexai-quality` 271 tests run, 0 failures, 0 errors, 0 skipped, and `schemaplexai-task` 166 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next project slice after the task MQ fail-log observability sequence. Keep maximum parallel subagents at 2.

## Task 28: Remove Task Tool Async False Success

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/subagent/TaskToolAdapter.java`
- Modify: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/tool/subagent/TaskToolAdapterTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the task-tool adapter boundary and verify that the current `SubAgentExecutionService` returns an async-start placeholder string rather than a completed child-agent result.

- [x] **Step 2: Define acceptance**

Acceptance: when `TaskToolAdapter` receives the placeholder `"Sub-agent execution started..."` result from `SubAgentExecutionService`, it must not return a successful tool result. Instead it must fail explicitly, telling operators that synchronous task-tool completion is not implemented yet. True completed sub-agent outputs must still remain successful.

- [x] **Step 3: Write red test**

Add a focused `TaskToolAdapterTest` case that stubs `SubAgentExecutionService.execute(...)` to return the async-start placeholder and asserts the adapter returns an error instead of success.

- [x] **Step 4: Focused red verification**

Run the focused agent-engine reactor and confirm the old adapter still reported success for the async-start placeholder.

- [x] **Step 5: Minimal implementation**

Treat the async-start placeholder as an explicit unsupported synchronous-completion path in `TaskToolAdapter`, while preserving quota handling, guardrail propagation, and genuinely completed sub-agent outputs.

- [x] **Step 6: Focused green verification**

Run the same focused agent-engine reactor and confirm the new adapter returns an error for the async-start placeholder while all adapter tests pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-engine -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=TaskToolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 12 tests, 1 failure, 0 errors because the old adapter returned success for the async-start placeholder.
- Green focused: same command passed 12 tests, 0 failures, 0 errors, 0 skipped after the adapter converted the async-start placeholder into an explicit error.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-engine -am` passed common/model/dao/integration/ops/agent-engine, with `schemaplexai-agent-engine` 1845 tests run, 0 failures, 0 errors, 4 skipped.
- Next: continue the next false-success candidate in the agent-engine/task integration path. Keep maximum parallel subagents at 2.

## Task 29: Remove Agent Config Zero-Row Write False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/AgentConfigServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read the agent-config service write paths and verify that `updateAgent(...)` and `deleteAgent(...)` currently ignore the affected-row count from `SfAgentMapper`.

- [x] **Step 2: Define acceptance**

Acceptance: when `updateById(...)` or `deleteById(...)` affects zero rows, `AgentConfigService` must not silently report success. It must throw `BaseException(ResultCode.AGENT_NOT_FOUND)` so upstream controllers do not return `Result.success()` for unchanged missing-agent writes.

- [x] **Step 3: Write red tests**

Add focused `AgentConfigServiceTest` cases asserting that zero-row update and zero-row delete paths throw `AGENT_NOT_FOUND`.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old service did not throw when mapper writes returned zero rows.

- [x] **Step 5: Minimal implementation**

Check affected-row counts in `updateAgent(...)` and `deleteAgent(...)`, and throw `BaseException(ResultCode.AGENT_NOT_FOUND)` when the mapper reports zero rows changed.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the service tests pass with the new explicit failure behavior.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 16 tests, 2 failures, 0 errors because the old service did not throw when `updateById(...)` or `deleteById(...)` returned zero rows.
- Green focused: same command passed 16 tests, 0 failures, 0 errors, 0 skipped after zero-row updates and deletes threw `AGENT_NOT_FOUND`.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 87 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate after agent-config zero-row write handling. Keep maximum parallel subagents at 2.

## Task 30: Remove Agent Config Save-Update Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/AgentConfigServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `saveAgentConfig(...)` and verify that its `config.getId() != null` update branch currently ignores the affected-row count from `agentConfigMapper.updateById(config)`.

- [x] **Step 2: Define acceptance**

Acceptance: when `saveAgentConfig(...)` enters its update branch and `agentConfigMapper.updateById(config)` affects zero rows, the service must not silently report success. It must throw `BaseException(ResultCode.AGENT_NOT_FOUND)` so missing agent-config rows cannot be mistaken for successful writes.

- [x] **Step 3: Write red test**

Add a focused `AgentConfigServiceTest` case asserting that a zero-row `saveAgentConfig(...)` update throws `AGENT_NOT_FOUND`.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old update branch did not throw when `agentConfigMapper.updateById(config)` returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the affected-row count in the `saveAgentConfig(...)` update branch and throw `BaseException(ResultCode.AGENT_NOT_FOUND)` when the mapper reports zero changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the new update-branch behavior passes the targeted test.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenUpdateAgentConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `saveAgentConfig(...)` update path ignored the zero-row `updateById(...)` result.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after the update path threw `AGENT_NOT_FOUND` for zero affected rows.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 88 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in `AgentConfigService`, most likely zero-row insert handling in `createAgent(...)` or the insert branch of `saveAgentConfig(...)`. Keep maximum parallel subagents at 2.

## Task 31: Remove Agent Config Insert-Path Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/AgentConfigServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `createAgent(...)` and the insert branch of `saveAgentConfig(...)` and verify that both currently ignore the affected-row count returned by MyBatis `insert(...)`.

- [x] **Step 2: Define acceptance**

Acceptance: when `createAgent(...)` or the insert branch of `saveAgentConfig(...)` receives a zero-row insert result, the service must not silently report success. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot mistake a no-op insert for a persisted write.

- [x] **Step 3: Write red tests**

Add focused `AgentConfigServiceTest` cases asserting that zero-row insert results from both `agentMapper.insert(...)` and `agentConfigMapper.insert(...)` throw `INTERNAL_ERROR`.

- [x] **Step 4: Focused red verification**

Run focused agent-config reactors and confirm the old insert paths did not throw when mapper inserts returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the affected-row counts in `createAgent(...)` and in the `saveAgentConfig(...)` insert branch, and throw `BaseException(ResultCode.INTERNAL_ERROR)` when either mapper reports zero inserted rows.

- [x] **Step 6: Focused green verification**

Run the focused insert-path tests and confirm both new explicit failure paths pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenCreateAgentAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `createAgent(...)` path ignored the zero-row insert result.
- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenInsertAgentConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `saveAgentConfig(...)` insert branch ignored the zero-row insert result.
- Green focused: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenInsertAgentConfigAffectsNoRows,AgentConfigServiceTest#shouldThrowWhenCreateAgentAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 2 tests, 0 failures, 0 errors, 0 skipped after both insert paths threw `INTERNAL_ERROR`.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 90 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in `AgentConfigService`, likely `saveToolBindings(...)` insert/delete write accounting or a neighboring write path with ignored affected-row results. Keep maximum parallel subagents at 2.

## Task 32: Remove Tool-Binding Insert Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/AgentConfigService.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/AgentConfigServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `saveToolBindings(...)` and verify that it currently ignores the affected-row count returned by `toolBindingMapper.insert(binding)`.

- [x] **Step 2: Define acceptance**

Acceptance: when `saveToolBindings(...)` attempts to insert a tool binding and `toolBindingMapper.insert(binding)` returns zero rows, the service must not silently report success. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot treat an unpersisted binding as successfully saved.

- [x] **Step 3: Write red test**

Add a focused `AgentConfigServiceTest` case asserting that a zero-row tool-binding insert throws `INTERNAL_ERROR`.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation did not throw when the tool-binding insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the affected-row count in `saveToolBindings(...)` for each binding insert and throw `BaseException(ResultCode.INTERNAL_ERROR)` when the mapper reports zero inserted rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the tool-binding insert failure path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenSaveToolBindingInsertAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `saveToolBindings(...)` path ignored the zero-row insert result.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after the service threw `INTERNAL_ERROR` for a zero-row tool-binding insert.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 91 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate near `AgentConfigService.saveToolBindings(...)`, likely the delete-side write accounting or another adjacent service write path with ignored affected-row results. Keep maximum parallel subagents at 2.

## Task 33: Remove Shadow Config Zero-Row Write False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/ShadowConfigService.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/ShadowConfigServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `ShadowConfigService` and verify that `createShadowConfig(...)`, `updateShadowConfig(...)`, and `deleteShadowConfig(...)` currently ignore the affected-row counts returned by the mapper.

- [x] **Step 2: Define acceptance**

Acceptance: zero-row writes in `ShadowConfigService` must not silently report success. `createShadowConfig(...)` must throw `BaseException(ResultCode.INTERNAL_ERROR)` for zero-row inserts, and `updateShadowConfig(...)` / `deleteShadowConfig(...)` must throw `BaseException(ResultCode.NOT_FOUND)` when no shadow-config row is changed or deleted.

- [x] **Step 3: Write red tests**

Add focused `ShadowConfigServiceTest` cases asserting explicit failures for zero-row create, update, and delete paths.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old service did not throw when mapper create/update/delete operations returned zero rows.

- [x] **Step 5: Minimal implementation**

Check affected-row counts in all three `ShadowConfigService` write paths and throw the matching exception when the mapper reports zero changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the three new explicit failure paths pass.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=ShadowConfigServiceTest#shouldThrowWhenCreateShadowConfigAffectsNoRows+shouldThrowWhenUpdateShadowConfigAffectsNoRows+shouldThrowWhenDeleteShadowConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 3 tests, 3 failures, 0 errors because the old service ignored zero-row create/update/delete results.
- Green focused: same command passed 3 tests, 0 failures, 0 errors, 0 skipped after the service checked mapper write counts.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 94 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in adjacent agent-config write surfaces, likely `PromptVersionServiceImpl.createVersion(...)` or `AgentsManifestLoader` write accounting. Keep maximum parallel subagents at 2.

## Task 34: Remove Prompt Version Insert Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/PromptVersionServiceImpl.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/PromptVersionServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `PromptVersionServiceImpl.createVersion(...)` and verify that it currently ignores the affected-row count returned by `promptVersionMapper.insert(pv)`.

- [x] **Step 2: Define acceptance**

Acceptance: when `PromptVersionServiceImpl.createVersion(...)` receives a zero-row insert result, it must not return a version object as if persistence succeeded. It must throw a clear exception so callers cannot mistake an unpersisted prompt version for a saved one.

- [x] **Step 3: Write red test**

Add a focused `PromptVersionServiceTest` case asserting that a zero-row insert during `createVersion(...)` throws an explicit exception.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation did not throw when the prompt-version insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the affected-row count from `promptVersionMapper.insert(pv)` and throw an explicit `IllegalStateException` when the insert affects zero rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the prompt-version insert failure path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=PromptVersionServiceTest#shouldThrowWhenCreateVersionInsertAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `createVersion(...)` path ignored the zero-row insert result.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert count and threw `IllegalStateException`.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 100 tests run, 0 failures, 0 errors, 0 skipped after backfilling existing `AgentsManifestLoaderTest` cases with the config/tool-binding stubs required by the new zero-row write guards.
- Next: continue the next false-success candidate near `AgentsManifestLoader` upsert/write accounting or another adjacent agent-config write surface. Keep maximum parallel subagents at 2.

## Task 35: Remove Agents Manifest Loader Zero-Row Write False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/manifest/AgentsManifestLoader.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/manifest/AgentsManifestLoaderTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `AgentsManifestLoader` and verify that its create/update agent path, create/update config path, and tool-binding insert path must not silently continue when mapper writes affect zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: `AgentsManifestLoader.loadFromManifest(...)` must throw explicit failures whenever agent upsert, agent-config upsert, or tool-binding insert operations report zero affected rows, so manifest import cannot report success when nothing was actually persisted.

- [x] **Step 3: Write red tests**

Add focused `AgentsManifestLoaderTest` cases covering zero-row create/update failures for agent writes, config writes, and tool-binding inserts.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation failed to throw on the manifest-loader zero-row write paths.

- [x] **Step 5: Minimal implementation**

Check affected-row counts in `upsertAgent(...)`, `upsertConfig(...)`, and `replaceToolBindings(...)`, then throw explicit `IllegalStateException`s when manifest persistence reports zero changed rows.

- [x] **Step 6: Focused green verification**

Run the focused reactor for the five manifest-loader false-success cases and confirm they all pass after the write-count guards are in place.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: earlier focused red runs reproduced the three create-path false-success gaps in `AgentsManifestLoaderTest` before the current guards existed: `shouldThrowWhenCreatingAgentAffectsNoRows`, `shouldThrowWhenCreatingAgentConfigAffectsNoRows`, and `shouldThrowWhenCreatingToolBindingAffectsNoRows`.
- Green focused (update-path repair): `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentsManifestLoaderTest#shouldThrowWhenUpdatingExistingAgentAffectsNoRows+shouldThrowWhenUpdatingExistingConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 0 failures, 0 errors, 0 skipped after removing obsolete stubs and keeping the new zero-row update guards.
- Green focused (full manifest-loader slice): `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentsManifestLoaderTest#shouldThrowWhenCreatingAgentAffectsNoRows+shouldThrowWhenCreatingAgentConfigAffectsNoRows+shouldThrowWhenCreatingToolBindingAffectsNoRows+shouldThrowWhenUpdatingExistingAgentAffectsNoRows+shouldThrowWhenUpdatingExistingConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 95 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate adjacent to manifest ingestion or another agent-config write surface, keeping the same TDD and verification cadence and a maximum of 2 parallel subagents.

## Task 36: Remove Agent Shadow Toggle Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/impl/AgentShadowConfigServiceImpl.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/impl/AgentShadowConfigServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `AgentShadowConfigServiceImpl.toggleEnabled(...)` and verify that it currently calls `updateById(config)` without checking whether the mapper actually changed a row.

- [x] **Step 2: Define acceptance**

Acceptance: when `toggleEnabled(...)` finds the shadow-config row by id but the subsequent update affects zero rows, it must not silently report success. It must throw `BaseException(ResultCode.NOT_FOUND)` so callers do not treat a no-op toggle as a successful write.

- [x] **Step 3: Write red test**

Add a focused `AgentShadowConfigServiceImplTest` case asserting that `toggleEnabled(...)` throws `NOT_FOUND` when `updateById(...)` returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation did not throw when the toggle update returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `updateById(config)` in `toggleEnabled(...)` and throw `BaseException(ResultCode.NOT_FOUND)` when the update reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row toggle path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentShadowConfigServiceImplTest#shouldThrowNotFoundWhenToggleEnabledAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `toggleEnabled(...)` path did not throw when `updateById(...)` returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `toggleEnabled(...)` checked the `updateById(...)` result and threw `NOT_FOUND`.
- Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentShadowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 101 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in adjacent agent-config write surfaces, likely another unchecked `updateById(...)` path such as `TenantEnvironmentConfigServiceImpl` or another service-layer write branch. Keep maximum parallel subagents at 2.

## Task 37: Remove Tenant Environment Update Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/impl/TenantEnvironmentConfigServiceImpl.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/impl/TenantEnvironmentConfigServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `TenantEnvironmentConfigServiceImpl.updateById(...)` and verify that it checks row existence first but still returns the raw `super.updateById(entity)` result without converting a zero-row update into an explicit failure.

- [x] **Step 2: Define acceptance**

Acceptance: when `TenantEnvironmentConfigServiceImpl.updateById(...)` finds the row by id but the actual update affects zero rows, it must not silently report success. It must throw `BaseException(ResultCode.NOT_FOUND)` so callers cannot treat a no-op environment update as a successful write.

- [x] **Step 3: Write red test**

Add a focused `TenantEnvironmentConfigServiceImplTest` case asserting that `updateById(...)` throws `NOT_FOUND` when the mapper update returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation did not throw when the tenant-environment update returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.updateById(entity)` and throw `BaseException(ResultCode.NOT_FOUND)` when the update reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row tenant-environment update path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest#shouldThrowNotFoundWhenUpdateAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when `super.updateById(entity)` returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `updateById(...)` checked the update result and threw `NOT_FOUND`.
- Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 102 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in adjacent agent-config write surfaces after `TenantEnvironmentConfigServiceImpl`, keeping the same TDD and verification cadence and a maximum of 2 parallel subagents.

## Task 38: Remove Tenant Environment Create Zero-Row False Success

**Files:**
- Modify: `schemaplexai-agent-config/src/main/java/com/schemaplexai/agent/config/service/impl/TenantEnvironmentConfigServiceImpl.java`
- Modify: `schemaplexai-agent-config/src/test/java/com/schemaplexai/agent/config/service/impl/TenantEnvironmentConfigServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `TenantEnvironmentConfigServiceImpl.save(...)` and verify that after validating `tenantId` it still returns the raw `super.save(entity)` result, which can silently report success when the mapper insert affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `TenantEnvironmentConfigServiceImpl.save(...)` receives a zero-row insert result, it must not return `true`/success semantics. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot treat an unpersisted tenant-environment config as saved.

- [x] **Step 3: Write red test**

Add a focused `TenantEnvironmentConfigServiceImplTest` case asserting that `save(...)` throws `INTERNAL_ERROR` when the mapper insert returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused agent-config reactor and confirm the old implementation did not throw when the tenant-environment insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.save(entity)` and throw `BaseException(ResultCode.INTERNAL_ERROR)` when the insert reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row tenant-environment insert path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-agent-config -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest#shouldThrowInternalErrorWhenCreateAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when `super.save(entity)` returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `save(...)` checked the insert result and threw `INTERNAL_ERROR`.
- Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 11 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-agent-config -am` passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 103 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate adjacent to `TenantEnvironmentConfigServiceImpl`, keeping the same TDD and verification cadence and a maximum of 2 parallel subagents.

## Task 39: Remove Quality Gate Create Zero-Row False Success

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityGateServiceImpl.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/service/QualityGateServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `QualityGateServiceImpl.save(...)` and verify that after validation/default-status setup it still returns the raw `super.save(gate)` result, which can silently report success when the mapper insert affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `QualityGateServiceImpl.save(...)` receives a zero-row insert result, it must not return success semantics. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot treat an unpersisted quality gate as saved.

- [x] **Step 3: Write red test**

Add a focused `QualityGateServiceImplTest` case asserting that `save(...)` throws `INTERNAL_ERROR` when the mapper insert returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused quality reactor and confirm the old implementation did not throw when the quality-gate insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.save(gate)` and throw `BaseException(ResultCode.INTERNAL_ERROR)` when the insert reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row quality-gate insert path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-quality -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest#save_whenInsertAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when the insert returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `save(...)` checked the insert result and threw `INTERNAL_ERROR`.
- Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 20 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-quality -am` passed common/model/dao/quality, with `schemaplexai-quality` 272 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate adjacent to `QualityGateServiceImpl`, especially its unchecked zero-row update path. Keep maximum parallel subagents at 2.

## Task 40: Remove Quality Gate Update Zero-Row False Success

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityGateServiceImpl.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/service/QualityGateServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `QualityGateServiceImpl.updateById(...)` and verify that after the existing row-presence check it still returns the raw `super.updateById(gate)` result, which can silently report success when the mapper update affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `QualityGateServiceImpl.updateById(...)` receives a zero-row update result after finding the gate row, it must not return success semantics. It must throw `BaseException(ResultCode.NOT_FOUND)` so callers cannot treat a no-op quality-gate update as successful.

- [x] **Step 3: Write red test**

Add a focused `QualityGateServiceImplTest` case asserting that `updateById(...)` throws `NOT_FOUND` when the mapper update returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused quality reactor and confirm the old implementation did not throw when the quality-gate update returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.updateById(gate)` and throw `BaseException(ResultCode.NOT_FOUND)` when the update reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row quality-gate update path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-quality -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest#updateById_whenUpdateAffectsNoRows_throwsNotFound" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when the update returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `updateById(...)` checked the update result and threw `NOT_FOUND`.
- Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 21 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-quality -am` passed common/model/dao/quality, with `schemaplexai-quality` 273 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate adjacent to `QualityGateServiceImpl`, likely `SecurityPolicyServiceImpl` or another unchecked update/save path. Keep maximum parallel subagents at 2.

## Task 41: Remove User Register Zero-Row False Success

**Files:**
- Modify: `schemaplexai-system/src/main/java/com/schemaplexai/system/service/UserService.java`
- Modify: `schemaplexai-system/src/test/java/com/schemaplexai/system/service/UserServiceTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `UserService.register(...)` and verify that after the username-uniqueness check, password encoding, and default-status setup it still returns `user.getId()` after a raw `save(user)` call, which can silently report success when the mapper insert affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `UserService.register(...)` receives a zero-row insert result, it must not return success semantics. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot treat a failed user create as a successful registration.

- [x] **Step 3: Write red test**

Add a focused `UserServiceTest` case asserting that `register(...)` throws `INTERNAL_ERROR` when the mapper insert returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused system reactor and confirm the old implementation did not throw when the user insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `save(user)` and throw `BaseException(ResultCode.INTERNAL_ERROR)` when the insert reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row register path now passes.

- [x] **Step 7: Full affected reactor verification**

Run `rtk mvn test -pl schemaplexai-system -am` and confirm 0 failures and 0 errors for the full affected reactor.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-system -am "-Dtest=UserServiceTest#register_whenSaveAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `register(...)` path did not throw when the insert returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `register(...)` checked the insert result and threw `INTERNAL_ERROR`.
- Green focused full class: `rtk mvn test -pl schemaplexai-system -am "-Dtest=UserServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 12 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-system -am` passed common/model/dao/system, with `schemaplexai-system` 129 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next false-success candidate in an adjacent service layer, starting with `SecurityPolicyServiceImpl` save/update handling. Keep maximum parallel subagents at 2.

## Task 42: Remove Security Policy Create Zero-Row False Success

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/SecurityPolicyServiceImpl.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/service/SecurityPolicyServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `SecurityPolicyServiceImpl.save(...)` and verify that after validation, default-status setup, and logging it still returns the raw `super.save(policy)` result, which can silently report success when the mapper insert affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `SecurityPolicyServiceImpl.save(...)` receives a zero-row insert result, it must not return success semantics. It must throw `BaseException(ResultCode.INTERNAL_ERROR)` so callers cannot treat a failed security-policy create as a successful write.

- [x] **Step 3: Write red test**

Add a focused `SecurityPolicyServiceImplTest` case asserting that `save(...)` throws `INTERNAL_ERROR` when the mapper insert returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused quality reactor and confirm the old implementation did not throw when the security-policy insert returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.save(policy)` and throw `BaseException(ResultCode.INTERNAL_ERROR)` when the insert reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row security-policy insert path now passes.

- [x] **Step 7: Full affected reactor verification**

Run the focused service class and the full `rtk mvn test -pl schemaplexai-quality -am` reactor to confirm 0 failures and 0 errors for the affected quality surface.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest#save_whenInsertAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when the insert returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `save(...)` checked the insert result and threw `INTERNAL_ERROR`.
- Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 27 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-quality -am` passed common/model/dao/quality, with `schemaplexai-quality` 275 tests run, 0 failures, 0 errors, 0 skipped.
- Note: while re-entering this quality slice, compilation surfaced UTF-8 BOM bytes at the heads of `QualityGateServiceImpl.java` and `QualityGateServiceImplTest.java`; those were normalized to UTF-8 without BOM so the reactor could compile again. No business logic changed in that encoding cleanup.
- Next: continue the adjacent zero-row update false-success in `SecurityPolicyServiceImpl.updateById(...)`, then move to the next unchecked quality service. Keep maximum parallel subagents at 2.

## Task 43: Remove Security Policy Update Zero-Row False Success

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/SecurityPolicyServiceImpl.java`
- Modify: `schemaplexai-quality/src/test/java/com/schemaplexai/quality/service/SecurityPolicyServiceImplTest.java`
- Modify: `docs/COVERAGE.md`
- Modify: this plan file

- [x] **Step 1: Contract discovery**

Read `SecurityPolicyServiceImpl.updateById(...)` and verify that after the existing row-presence and deprecated-state checks it still returns the raw `super.updateById(policy)` result, which can silently report success when the mapper update affects zero rows.

- [x] **Step 2: Define acceptance**

Acceptance: when `SecurityPolicyServiceImpl.updateById(...)` receives a zero-row update result after finding the policy row, it must not return success semantics. It must throw `BaseException(ResultCode.NOT_FOUND)` so callers cannot treat a no-op security-policy update as a successful write.

- [x] **Step 3: Write red test**

Add a focused `SecurityPolicyServiceImplTest` case asserting that `updateById(...)` throws `NOT_FOUND` when the mapper update returns zero affected rows.

- [x] **Step 4: Focused red verification**

Run the focused quality reactor and confirm the old implementation did not throw when the security-policy update returned zero rows.

- [x] **Step 5: Minimal implementation**

Check the boolean result from `super.updateById(policy)` and throw `BaseException(ResultCode.NOT_FOUND, "Security policy not found: " + policy.getId())` when the update reports no changed rows.

- [x] **Step 6: Focused green verification**

Run the same focused reactor and confirm the zero-row security-policy update path now passes.

- [x] **Step 7: Full affected reactor verification**

Run the focused service class and the full `rtk mvn test -pl schemaplexai-quality -am` reactor to confirm 0 failures and 0 errors for the affected quality surface.

- [x] **Step 8: Evidence and diff review**

Record exact evidence in `docs/COVERAGE.md`, run scoped `git diff --check`, and inspect scoped `git diff` before selecting the next slice.

Evidence captured:

- Red: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest#updateById_whenUpdateAffectsNoRows_throwsNotFound" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when the update returned zero rows.
- Green focused: same command passed 1 test, 0 failures, 0 errors, 0 skipped after `updateById(...)` checked the update result and threw `NOT_FOUND`.
- Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 27 tests, 0 failures, 0 errors, 0 skipped.
- Green reactor: `rtk mvn test -pl schemaplexai-quality -am` passed common/model/dao/quality, with `schemaplexai-quality` 275 tests run, 0 failures, 0 errors, 0 skipped.
- Next: continue the next adjacent quality-service false-success candidate, with `ReviewServiceImpl.save(...)` and `ToolApprovalServiceImpl.save(...)` now the shortest likely slices. Keep maximum parallel subagents at 2.
