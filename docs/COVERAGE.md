# SchemaPlexAI Coverage Baseline

**Date:** 2026-05-06  
**Generated from:** `mvn clean verify` with JaCoCo 0.8.12

> Update 2026-05-21: this coverage percentage baseline is stale. A fresh
> focused backend test run passed with `mvn test -pl schemaplexai-web -am`
> across the 10-module web reactor, including 140 tests in `schemaplexai-web`.
> A full `mvn clean verify` is still needed to refresh JaCoCo percentages.
>
> Update 2026-05-25: the agent-engine startup-smoke slice is freshly verified.
> `rtk mvn test -pl schemaplexai-agent-engine -am` passed across the common,
> model, dao, integration, ops, and agent-engine reactor with 1843 agent-engine
> tests run, 0 failures, 0 errors, and 4 skipped. The percentage table above is
> still the 2026-05-06 JaCoCo baseline until a fresh `mvn clean verify` is run.

---

## Summary

| Module | Instruction % | Branch % | Line % |
|--------|--------------|----------|--------|
| schemaplexai-agent-engine | 92.9% | 75.0% | 93.2% |
| schemaplexai-gateway | 83.8% | 66.7% | 82.8% |
| schemaplexai-common | 81.3% | 72.7% | 82.9% |
| schemaplexai-dao | 44.6% | 50.0% | 44.4% |
| schemaplexai-model | 31.3% | 27.3% | 30.4% |
| schemaplexai-agent-config | 2.8% | 0.0% | 1.3% |
| schemaplexai-system | 1.6% | 0.0% | 2.9% |
| schemaplexai-web | 0.0% | 0.0% | 0.0% |
| schemaplexai-integration | 0.0% | 0.0% | 0.0% |
| **Average** | **37.6%** | **32.4%** | **37.5%** |

*Modules not listed (schemaplexai-context, schemaplexai-quality, schemaplexai-ops, schemaplexai-spec, schemaplexai-workflow, schemaplexai-task) have no JaCoCo reports — either no test sources exist or the build was skipped. schemaplexai-admin has tests (12) but JaCoCo report may not be generated yet.*

---

## Modules Above 80% Instruction Coverage

| Module | Instruction % | Test Count |
|--------|--------------|------------|
| schemaplexai-agent-engine | 92.9% | 154 (incl. 27 integration) |
| schemaplexai-gateway | 83.8% | 18 |
| schemaplexai-common | 81.3% | 40 |

These modules meet the 80% minimum threshold.

---

## Modules Below 80% Threshold

| Module | Instruction % | Gap to 80% | Action |
|--------|--------------|------------|--------|
| schemaplexai-dao | 44.6% | 35.4% | Add tests for MyBatis-Plus extensions |
| schemaplexai-model | 31.3% | 48.7% | Add tests for entity/DTO classes |
| schemaplexai-agent-config | 2.8% | 77.2% | Write full test suite for configuration CRUD |
| schemaplexai-system | 1.6% | 78.4% | Write tests for auth/tenant services |
| schemaplexai-web | 0.0% | 80.0% | Add tests for controllers, SSE, WebSocket |
| schemaplexai-integration | 0.0% | 80.0% | Add tests for webhook handlers, MCP clients |

---

## Backend Test Summary (Post-JaCoCo Verify)

```
schemaplexai-common:        40 passed
schemaplexai-model:         12 passed
schemaplexai-dao:           11 passed
schemaplexai-gateway:       18 passed
schemaplexai-web:          140 passed (2026-05-21 web reactor run)
schemaplexai-system:        129 passed (2026-05-24 system module run)
schemaplexai-agent-config:  85 passed (2026-05-24 agent-config reactor run)
schemaplexai-agent-engine: 1844 run, 0 failures, 0 errors, 4 skipped (2026-05-25 agent-engine reactor run)
schemaplexai-integration:    0 passed (no tests written)
schemaplexai-context:      228 passed (2026-05-24 context module run)
schemaplexai-quality:      271 passed (2026-05-25 quality reactor run)
schemaplexai-spec:         150 passed (2026-05-24 spec reactor run)
schemaplexai-ops:          228 passed (2026-05-24 ops reactor run)
schemaplexai-task:         155 passed (2026-05-25 task reactor run)
schemaplexai-workflow:     210 passed (2026-05-24 workflow reactor run)
schemaplexai-admin:        111 passed (2026-05-24 admin reactor run)
---
TOTAL BACKEND:             281 passed in the 2026-05-06 JaCoCo baseline
```

Additional verification log:

```
rtk mvn -pl schemaplexai-common "-Dtest=ObservabilityAutoConfigurationTest,OpenTelemetryTracingServiceTest,TenantIdSpanProcessorTest" test
  -> Observability auto-configuration + tracing focused tests:
     14 run, 0 failures, 0 errors, 0 skipped

rtk mvn -pl schemaplexai-agent-engine -am "-Dtest=EventReorderingConsumerTest,EventReorderingConsumerContextTest,GapRecoveryJobConditionalTest,OutboxPublisherConditionalTest,McpToolDiscoveryConditionalTest,SchemaPlexaiAgentEngineApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS
  -> Agent-engine startup-smoke + conditional background-task tests:
     13 run, 0 failures, 0 errors, 0 skipped

rtk mvn -pl schemaplexai-agent-engine -am "-Dtest=EventEntityMappingTest,UuidTypeHandlerTest,SchemaPlexaiAgentEngineApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS
  -> Execution event MyBatis-Plus primary-key mapping, composite processed-event
     mapper boundary, UUID type handler, and real startup smoke:
     7 run, 0 failures, 0 errors, 0 skipped
  -> Startup log no longer shows the prior `ExecutionEvent` / `ProcessedEvent`
     `Not found @TableId` warnings; only the expected DEV master-secret warning
     remains in this smoke configuration.

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS
  -> schemaplexai-common: 129 run, 0 failures, 0 errors, 0 skipped
  -> schemaplexai-model: 27 run, 0 failures, 0 errors, 0 skipped
  -> schemaplexai-dao: 20 run, 0 failures, 0 errors, 0 skipped
  -> schemaplexai-integration: 270 run, 0 failures, 0 errors, 0 skipped
  -> schemaplexai-ops: 228 run, 0 failures, 0 errors, 0 skipped
  -> schemaplexai-agent-engine: 1843 run, 0 failures, 0 errors, 4 skipped

mvn clean test -pl schemaplexai-agent-engine -Dtest=MigrationSmokeTest
  -> MigrationSmokeTest: 6 passed

mvn test -pl schemaplexai-web -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, agent-config, quality, web all SUCCESS
  -> schemaplexai-web: 140 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS
  -> 1820 tests run, 0 failures, 0 errors, 4 skipped

rtk mvn test -pl schemaplexai-agent-engine -Dtest=HttpCallAdapterTest
  -> HttpCallAdapterTest: 35 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=NotificationConsumerTest
  -> NotificationConsumerTest: 9 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=SchemaPlexaiTaskApplicationTest
  -> SchemaPlexaiTaskApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 122 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow -Dtest=HttpNodeExecutorTest
  -> HttpNodeExecutorTest: 5 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow -Dtest=DelegateTest
  -> DelegateTest: 30 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow -Dtest=ConditionNodeExecutorTest
  -> ConditionNodeExecutorTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow
  -> schemaplexai-workflow: 209 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -Dtest=McpToolAdapterTest
  -> McpToolAdapterTest: 7 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine "-Dtest=McpToolAdapterTest,McpToolDiscoveryTest,McpClientManagerTest,McpToolRefTest,McpWhitelistTest"
  -> MCP tool tests: 54 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS
  -> 1820 tests run, 0 failures, 0 errors, 4 skipped

rtk mvn test -pl schemaplexai-task -Dtest=QualityEventConsumerTest
  -> QualityEventConsumerTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 124 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=WorkflowTriggerConsumerTest
  -> WorkflowTriggerConsumerTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 126 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=MilvusSyncConsumerTest
  -> MilvusSyncConsumerTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 128 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=DeadLetterRetryServiceTest
  -> DeadLetterRetryServiceTest: 2 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 127 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -Dtest=ToolSandboxTest
  -> ToolSandboxTest: 10 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine "-Dtest=ToolSandboxTest,ToolCallingStateHandlerTest"
  -> ToolSandboxTest + ToolCallingStateHandlerTest: 34 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS after the container sandbox behavior change
  -> 1820 tests run, 0 failures, 0 errors, 4 skipped

rtk mvn test -pl schemaplexai-agent-engine -Dtest=GapRecoveryJobTest
  -> GapRecoveryJobTest: 3 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine "-Dtest=GapRecoveryJobTest,EventReorderingConsumerTest"
  -> GapRecoveryJobTest + EventReorderingConsumerTest: 7 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS after the gap alert routing change
  -> 1820 tests run, 0 failures, 0 errors, 4 skipped

rtk mvn test -pl schemaplexai-agent-engine "-Dtest=FeedbackTrendAnalyzerTest,ToolExecutionRecorderTest"
  -> FeedbackTrendAnalyzerTest + ToolExecutionRecorderTest: 15 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-engine -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine all SUCCESS after the feedback trend query change
  -> 1823 tests run, 0 failures, 0 errors, 4 skipped

rtk mvn test -pl schemaplexai-task -Dtest=ApprovalTimeoutJobTest
  -> ApprovalTimeoutJobTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 127 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task "-Dtest=ChatMessageArchiveJobTest,ChatMessageArchiveServiceTest"
  -> ChatMessageArchiveJobTest + ChatMessageArchiveServiceTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 130 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task "-Dtest=MilvusReconciliationJobTest,MilvusReconciliationServiceTest"
  -> MilvusReconciliationJobTest + MilvusReconciliationServiceTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 133 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=MilvusSyncConsumerTest
  -> MilvusSyncConsumerTest: 6 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 135 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=WorkflowTriggerConsumerTest
  -> WorkflowTriggerConsumerTest: 5 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 136 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=QualityEventConsumerTest
  -> QualityEventConsumerTest: 5 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 137 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -Dtest=CostStatisticsJobTest
  -> CostStatisticsJobTest: 2 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task
  -> schemaplexai-task: 138 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncDependencyConfigurationTest,CostSyncConsumerTest,CostStatisticsJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> CostSyncDependencyConfigurationTest + CostSyncConsumerTest + CostStatisticsJobTest:
     8 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-ops: 227 passed, 0 failures, 0 errors
  -> schemaplexai-task: 139 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am -Dtest=HealthCheckJobTest "-Dsurefire.failIfNoSpecifiedTests=false"
  -> HealthCheckJobTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-ops: 227 passed, 0 failures, 0 errors
  -> schemaplexai-task: 141 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am -Dtest=DeadLetterRetryServiceTest "-Dsurefire.failIfNoSpecifiedTests=false"
  -> DeadLetterRetryServiceTest: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 143 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=AuthServiceTest#login_blankTenantId_throwsParamErrorWithoutUserLookup
  -> AuthServiceTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=AuthServiceTest#login_*"
  -> AuthServiceTest login methods: 6 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=SystemControllerTest#auth_logout_passesBearerTokenToService
  -> SystemControllerTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=SystemControllerTest#auth_*"
  -> SystemControllerTest auth methods: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=TenantPolicyServiceTest#saveOrUpdatePolicy_blankTenantId_throwsParamErrorWithoutWriting
  -> TenantPolicyServiceTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=TenantPolicyServiceTest
  -> TenantPolicyServiceTest: 8 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=TenantPolicyServiceTest#saveOrUpdatePolicy_blankPolicyType_throwsParamErrorWithoutWriting
  -> TenantPolicyServiceTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system -Dtest=TenantPolicyServiceTest
  -> TenantPolicyServiceTest: 9 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=AuthServiceTest#login_*,SystemControllerTest#auth_*,TenantPolicyServiceTest"
  -> Affected system auth + tenant-policy tests: 19 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=AuthServiceTest#refreshToken_*"
  -> AuthServiceTest refresh methods: 3 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=AuthServiceTest#logout_*"
  -> AuthServiceTest logout methods: 2 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system "-Dtest=AuthServiceTest#isTokenBlacklisted_*,AuthServiceTest#blacklistToken_*"
  -> AuthServiceTest blacklist methods: 3 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-system
  -> schemaplexai-system: 129 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-spec -am "-Dtest=SchemaPlexaiSpecApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiSpecApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-spec -am
  -> Reactor modules: schemaplexai, common, model, dao, spec all SUCCESS
  -> schemaplexai-spec: 150 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-workflow -am "-Dtest=SchemaPlexaiWorkflowApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiWorkflowApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow -am
  -> Reactor modules: schemaplexai, common, model, dao, workflow all SUCCESS
  -> schemaplexai-workflow: 210 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow -Dtest=ScriptNodeExecutorTest
  -> ScriptNodeExecutorTest: 2 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow
  -> schemaplexai-workflow: 210 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow "-Dtest=NodeExecutorRegistryTest#aiModelNodeExecutor_*"
  -> NodeExecutorRegistryTest AI_MODEL methods: 4 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow
  -> schemaplexai-workflow: 210 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow "-Dtest=NodeExecutorRegistryTest#toolCallNodeExecutor_*"
  -> NodeExecutorRegistryTest TOOL_CALL methods: 3 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow
  -> schemaplexai-workflow: 210 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow "-Dtest=ConcurrentNodeExecutorTest"
  -> ConcurrentNodeExecutorTest: 8 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-workflow
  -> schemaplexai-workflow: 210 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-context "-Dtest=MilvusSyncServiceImplTest"
  -> MilvusSyncServiceImplTest: 11 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-context
  -> schemaplexai-context: 228 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality "-Dtest=QualityOrchestratorTest"
  -> QualityOrchestratorTest: 10 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality
  -> schemaplexai-quality: 262 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality "-Dtest=SecurityScanRuleTest#check_withExplicitFailedScanEvidence_reportsScanFailure"
  -> SecurityScanRuleTest explicit failed scan evidence: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality "-Dtest=SecurityScanRuleTest,SpecComplianceRuleTest"
  -> SecurityScanRuleTest + SpecComplianceRuleTest: 20 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality
  -> schemaplexai-quality: 269 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-quality -am "-Dtest=EscalationPolicyServiceSchedulingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> EscalationPolicyServiceSchedulingTest: 1 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-quality -am "-Dtest=SchemaPlexaiQualityApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiQualityApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-quality -am
  -> Reactor modules: schemaplexai, common, model, dao, quality all SUCCESS
  -> schemaplexai-quality: 262 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-ops -am "-Dtest=SchemaPlexaiOpsApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiOpsApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-ops -am
  -> Reactor modules: schemaplexai, common, model, dao, ops all SUCCESS
  -> schemaplexai-ops: 228 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-agent-config -am "-Dtest=SchemaPlexaiAgentConfigApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiAgentConfigApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-agent-config -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, agent-config all SUCCESS
  -> schemaplexai-agent-engine: 1823 passed, 0 failures, 0 errors, 4 skipped
  -> schemaplexai-agent-config: 85 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-admin -am "-Dtest=SchemaPlexaiAdminApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiAdminApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-admin -am
  -> Reactor modules: schemaplexai, common, model, dao, system, admin all SUCCESS
  -> schemaplexai-system: 128 passed, 0 failures, 0 errors
  -> schemaplexai-admin: 111 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-task -am "-Dtest=SchemaPlexaiTaskApplicationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> SchemaPlexaiTaskApplicationTest: 1 passed, 0 failures, 0 errors

rtk mvn -pl schemaplexai-task -am "-Dtest=HealthCheckJobTest#run_scheduleDelaysFirstCheckUntilApplicationHasStarted" "-Dsurefire.failIfNoSpecifiedTests=false" test
  -> HealthCheckJobTest schedule contract: 1 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 144 passed, 0 failures, 0 errors

rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest,CostSyncDependencyConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> CostSyncDependencyConfigurationTest + CostSyncConsumerTest:
     9 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 147 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> NotificationConsumerTest red run:
     10 tests run, 1 failure, 0 errors
  -> Expected old behavior: `InboxDeduplicationService.markProcessed` failure
     was swallowed, `basicAck(1L, false)` was called, and `basicNack` was not
     called after in-app notification persistence succeeded.

rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> NotificationConsumerTest green run:
     10 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 150 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=MemoryConsolidationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> MemoryConsolidationJobTest red run:
     5 tests run, 2 failures, 0 errors
  -> Expected old behavior: Redis `keys(...)` and `expire(...)` failures were
     logged and swallowed, so the scheduled run returned normally.

rtk mvn test -pl schemaplexai-task -am "-Dtest=MemoryConsolidationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> MemoryConsolidationJobTest green run:
     5 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 152 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=ApprovalTimeoutJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> ApprovalTimeoutJobTest red run:
     2 tests run, 1 failure, 0 errors
  -> Expected old behavior: `EscalationPolicyService.checkEscalations()`
     failure was logged and swallowed, so the scheduled run returned normally.

rtk mvn test -pl schemaplexai-task -am "-Dtest=ApprovalTimeoutJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> ApprovalTimeoutJobTest green run:
     2 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 153 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=ChatMessageArchiveJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> ChatMessageArchiveJobTest red run:
     2 tests run, 1 failure, 0 errors
  -> Expected old behavior: `ChatMessageArchiveService.archiveExpiredMessages(...)`
     failure was logged and swallowed, so the scheduled run returned normally.

rtk mvn test -pl schemaplexai-task -am "-Dtest=ChatMessageArchiveJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> ChatMessageArchiveJobTest green run:
     2 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 154 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusReconciliationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> MilvusReconciliationJobTest red run:
     2 tests run, 1 failure, 0 errors
  -> Expected old behavior: `MilvusReconciliationService.reconcilePendingDocuments(...)`
     failure was logged and swallowed, so the scheduled run returned normally.

rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusReconciliationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> MilvusReconciliationJobTest green run:
     2 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 155 run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am "-Dtest=DeadLetterHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> DeadLetterHandlerTest red run:
     4 tests run, 1 failure, 0 errors
  -> Expected old behavior: audit-event publish failure was logged and swallowed,
     so the dead-letter message was ACKed even though no audit event was emitted.

rtk mvn test -pl schemaplexai-task -am "-Dtest=DeadLetterHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
  -> DeadLetterHandlerTest green run:
     4 tests run, 0 failures, 0 errors, 0 skipped

rtk mvn test -pl schemaplexai-task -am
  -> Reactor modules: schemaplexai, common, model, dao, integration, ops,
     agent-engine, quality, task all SUCCESS
  -> schemaplexai-task: 155 run, 0 failures, 0 errors, 0 skipped
```

## Frontend Test Summary

```
73 total tests, 69 passed, 4 failed
Failing: 4 Layout.test.tsx (antd theme.useToken ContextProvider issue in jsdom)
```

---

## Notes

- **JaCoCo enabled** via root `pom.xml` `<plugins>` section (jacoco-maven-plugin 0.8.12, prepare-agent + report bound to verify phase).
- **Baseline established** — first time JaCoCo reports generated across all modules.
- **agent-engine** is the strongest module at 92.9% instruction coverage.
- **4 frontend Layout tests** are skipped due to an antd `ConfigProvider` / React Router `Outlet` compatibility issue in the jsdom test environment. The Layout component works correctly in the browser.
- **2026-05-21 web update**: `SchemaPlexaiWebApplicationTest` now loads the real Spring context instead of swallowing startup failures.
- **2026-05-21 web lifecycle update**: `ExecutionWebController` pause/resume/cancel now call a web-layer `ExecutionLifecyclePort`; the engine adapter delegates to `AgentExecutionLifecycleService` when present and fails explicitly instead of returning fake success when the engine runtime is unavailable.
- **2026-05-21 web execution status update**: `ExecutionWebController` status lookup now calls a web-layer `ExecutionStatusPort`; the engine adapter reads `SfAgentExecutionMapper`, maps persisted execution fields, validates execution ids, and returns 404 for unknown executions instead of hardcoded `RUNNING` progress data.
- **2026-05-21 web approval update**: `ApprovalWebController` list/approve/reject/escalate now call a web-layer `ApprovalWorkflowPort`; the quality adapter delegates to `ApprovalTicketService`, validates tenant/ticket identifiers, and fails explicitly instead of returning hardcoded or fake approval responses when the quality runtime is unavailable.
- **2026-05-21 web cost update**: `CostWebController` summary/execution endpoints now call a web-layer `CostQueryPort`; the ops adapter delegates to `CostService`, and `CostService` can aggregate per-execution cost records from PostgreSQL instead of returning fixed demo cost data.
- **2026-05-21 migration update**: `MigrationSmokeTest` now checks Flyway version uniqueness both inside agent-engine and across default `classpath:db/migration` module resources.
- **2026-05-21 agent-engine HTTP security update**: `HttpCallAdapter` now enforces optional tenant URL host allowlists from `TenantEnvironmentConfig.extraConfig.httpUrlAllowlist` before DNS resolution, including redirect targets. Exact hosts and wildcard subdomains such as `*.trusted.example` are covered by tests.
- **2026-05-21 agent-engine MCP update**: `McpToolAdapter` no longer reports fake success after whitelist/client validation when the underlying MCP protocol call has not been implemented. It now preserves validation behavior and returns an explicit execution error for unsupported protocol calls.
- **2026-05-21 task notification update**: `NotificationConsumer` now persists in-app notifications through `NotificationMapper` before ACK, nacks and logs persistence failures to the DLQ path, and `SchemaPlexaiTaskApplication` explicitly scans both task-local mappers and the DAO notification mapper.
- **2026-05-21 task quality-event update**: `QualityEventConsumer` now parses incoming JSON and rejects malformed payloads or messages missing `eventType` through the existing fail-log + DLQ path instead of ACKing unusable quality events.
- **2026-05-21 task workflow-trigger update**: `WorkflowTriggerConsumer` now parses incoming JSON and rejects malformed payloads or messages missing `workflowDefinitionKey` through the existing fail-log + DLQ path instead of ACKing unusable workflow trigger events.
- **2026-05-21 task Milvus-sync update**: `MilvusSyncConsumer` now parses incoming JSON and rejects malformed payloads or messages missing `collectionName` through the existing fail-log + DLQ path instead of ACKing unusable vector sync events.
- **2026-05-21 task Milvus-sync handling update**: `MilvusSyncConsumer` no longer ACKs valid sync messages after validation only. It now parses payloads into `MilvusSyncMessage`, validates `SYNC_DOC` requirements, delegates to `MilvusSyncRequestHandler`, and nacks/logs failures when no runtime handler is configured.
- **2026-05-21 task workflow-trigger handling update**: `WorkflowTriggerConsumer` no longer ACKs valid workflow trigger messages after validation only. It now parses payloads into `WorkflowTriggerMessage`, delegates to `WorkflowTriggerRequestHandler`, and nacks/logs failures when no runtime handler is configured.
- **2026-05-21 task quality-event handling update**: `QualityEventConsumer` no longer ACKs valid quality events after validation only. It now parses payloads into `QualityEventMessage`, delegates to `QualityEventRequestHandler`, and nacks/logs failures when no runtime handler is configured.
- **2026-05-21 task cost-statistics update**: `CostStatisticsJob` no longer reports completion while doing no work. It now reuses `CostDataSyncService.syncIncrementalData()` for the PG-to-ClickHouse cost sync path and then calls `CostService.checkBudgetAlerts()` to evaluate budget alert thresholds.
- **2026-05-21 ops/task cost-sync fallback update**: `CostSyncConsumer` and `CostStatisticsJob` now depend on `CostDataSyncService` instead of the conditionally registered `ClickHouseCostSyncService`. When `clickhouse.enabled=false` or missing, `DisabledCostDataSyncService` provides an explicit no-op fallback so task cost beans can still be assembled safely.
- **2026-05-21 task health-check update**: `HealthCheckJob` now checks Redis, PostgreSQL, and RabbitMQ independently instead of logging a Redis-only placeholder. Each dependency has isolated failure handling, so one failed probe does not prevent later checks. Milvus health remains future work because the task module does not yet expose a local Milvus client boundary.
- **2026-05-21 task dead-letter retry update**: `DeadLetterRetryService` now queries `sf_message_fail_log` for `PENDING` failed messages by AMQP `messageId`, republishes the original payload to its stored exchange/routing key, marks successful retries as `RETRIED`, increments `retryCount`, and lists recent pending failures with a bounded limit.
- **2026-05-21 agent-engine container sandbox update**: `ContainerToolSandbox` no longer reports fake sandbox execution success after validation and whitelist checks. It now fails explicitly until real container execution is implemented.
- **2026-05-21 agent-engine gap recovery update**: `GapRecoveryJob` now publishes explicit gap-detected alerts with generated event ids, routes them through `execution-gap.detected` instead of the `execution.#` event stream, and no longer describes missing sequence alerts as recovered events.
- **2026-05-21 agent-engine feedback trend update**: `FeedbackTrendAnalyzer` no longer returns empty trend/anomaly results solely because its history query was a placeholder. It now reads recent `TOOL_FAILURE` / `TOOL_BLOCKED` execution logs through `ToolExecutionRecorder`, parses the existing recorder message format, and logs anomaly rates without SLF4J placeholder drift.
- **2026-05-21 task approval-timeout update**: `ApprovalTimeoutJob` no longer logs a synthetic timeout threshold and reports completion without work. It now delegates scheduled checks to `EscalationPolicyService.checkEscalations()`, reusing the quality module's pending approval SLA scan.
- **2026-05-21 task chat-archive update**: `ChatMessageArchiveJob` no longer reports archive completion while doing no work. It now delegates to `ChatMessageArchiveService`, which transactionally copies expired `sf_chat_message` rows into `sf_chat_message_archive` with idempotent `NOT EXISTS` protection, then deletes hot rows already present in the archive table.
- **2026-05-21 task Milvus-reconciliation update**: `MilvusReconciliationJob` no longer reports vector reconciliation completion while doing no work. It now delegates to `MilvusReconciliationService`, which scans `sf_knowledge_doc` rows with `sync_status` `PENDING` / `FAILED` and dispatches document sync requests to the existing `sf.milvus.sync` routing key.
- **2026-05-21 workflow HTTP node update**: `HttpNodeExecutor` no longer returns a fake `200/placeholder` response when the node URL is missing or blank; it now returns an explicit failure so misconfigured workflows do not advance as successful HTTP calls.
- **2026-05-21 workflow spec-review notification update**: `SpecReviewNotificationDelegate` now preserves the workflow audit payload and also publishes numeric submitter notifications to `sf.exchange` / `sf.notification` using the existing in-app notification message contract.
- **2026-05-21 workflow condition-node update**: `ConditionNodeExecutor` now resolves boolean literals such as `true` and `false`, so expressions like `approved == true` branch correctly instead of treating boolean literals as missing variables.
- **2026-05-25 workflow script-node update**: `ScriptNodeExecutor` no longer reports placeholder success with `"script executed"` while no script runtime exists. It now returns an explicit failure telling callers to configure a script runtime before enabling SCRIPT nodes, preventing workflows from advancing through unimplemented script steps as successful work. TDD evidence: red `ScriptNodeExecutorTest#execute_failsExplicitlyUntilRuntimeIsImplemented` failed because the old executor returned success; green focused `ScriptNodeExecutorTest` 2 passed; green full `schemaplexai-workflow` module 210 passed.
- **2026-05-25 workflow AI-model node update**: `AIModelNodeExecutor` no longer falls back to a simulated AI response when `agent-engine` is unavailable, unconfigured, or returns no `data`. It now fails explicitly unless the real agent-engine call returns data, preventing AI_MODEL workflow nodes from advancing as successful generated work after a connection or runtime failure. TDD evidence: red `NodeExecutorRegistryTest#aiModelNodeExecutor_*` failed because the old executor returned success; green focused `NodeExecutorRegistryTest#aiModelNodeExecutor_*` 4 passed; green full `schemaplexai-workflow` module 210 passed.
- **2026-05-25 workflow tool-call node update**: `ToolCallNodeExecutor` no longer returns a fabricated `toolResult` with `status=executed` when no tool runtime exists. It now preserves required `toolName` validation and otherwise fails explicitly until a real tool runtime is configured, preventing TOOL_CALL workflow nodes from advancing as successful external-tool work. TDD evidence: red `NodeExecutorRegistryTest#toolCallNodeExecutor_*` failed because the old executor returned success; green focused `NodeExecutorRegistryTest#toolCallNodeExecutor_*` 3 passed; green full `schemaplexai-workflow` module 210 passed.
- **2026-05-24 workflow concurrent-node update**: `ConcurrentNodeExecutor` no longer simulates successful sub-task outputs. It validates `subTasks` and each sub-task `prompt`, then fails explicitly until a real concurrent task runtime is configured, preventing CONCURRENT workflow nodes from advancing as successful parallel work without an execution backend. TDD evidence: red `ConcurrentNodeExecutorTest#singleSubTask_withoutRuntime_returnsFailure` failed because the old executor returned simulated success; green focused `ConcurrentNodeExecutorTest` 8 passed; green full `schemaplexai-workflow` module 210 passed.
- **2026-05-24 context Milvus extraction false-success update**: `MilvusSyncServiceImpl` no longer falls back to simulated document text when MinIO download/Tika extraction fails or when a knowledge document has no `fileUrl`. It now records the document as failed through `FailedStatusWriter`, propagates a `BaseException`, and avoids chunking, embedding, or Milvus insertion unless real file text is extracted. TDD evidence: red `MilvusSyncServiceImplTest#syncToMilvus_minioExtractionFailure_marksFailedWithoutSimulatedSync` exposed the old simulated sync; red `MilvusSyncServiceImplTest#syncToMilvus_missingFileUrl_marksFailedWithoutSimulatedSync` exposed the old missing-file simulated extraction; green focused `MilvusSyncServiceImplTest` 11 passed; green full `schemaplexai-context` module 228 passed.
- **2026-05-24 quality orchestrator false-success update**: `QualityOrchestrator` no longer treats unknown rule names or invalid gate `rulesJson` as an empty successful rule set. Unknown rules and parse failures now produce `CRITICAL` failed check results so a quality gate cannot pass when its configured checks are missing or unreadable. TDD evidence: red `QualityOrchestratorTest#evaluate_unknownRule_returnsFailedReport` and `QualityOrchestratorTest#evaluate_parseError_returnsFailedReport` exposed the prior all-passed behavior; green focused `QualityOrchestratorTest` 10 passed; green full `schemaplexai-quality` module 262 passed.
- **2026-05-24 quality rule evidence update**: `SecurityScanRule` no longer passes solely because obvious secret patterns are absent; it now requires explicit security-scan evidence and fails on findings or `securityScanPassed=false`. `SpecComplianceRule` no longer passes solely because a `specId` exists; it now requires explicit compliance-checked and compliance-passed evidence with no violations. TDD evidence: red `SecurityScanRuleTest#check_withoutSecurityScanEvidence_failsInsteadOfPlaceholderPass` exposed the old safe-output placeholder pass; red `SpecComplianceRuleTest#check_withSpecIdButNoComplianceEvidence_failsInsteadOfPlaceholderPass` exposed the old `specId` placeholder pass; red `SecurityScanRuleTest#check_withExplicitFailedScanEvidence_reportsScanFailure` clarified explicit failed-scan reporting; green focused `SecurityScanRuleTest,SpecComplianceRuleTest` 20 passed; green full `schemaplexai-quality` module 269 passed.
- **2026-05-24 workflow startup-smoke update**: `SchemaPlexaiWorkflowApplicationTest` now starts the real Workflow application with `--server.port=0`, an H2 test datasource, Flowable async execution disabled, and a test-only `jwt.secret`, asserting both the running Spring context and a real Flowable `RepositoryService` bean. TDD evidence: red focused startup test failed when `MODE=PostgreSQL` made Flowable's H2 schema script reject `IDENTITY`; green focused startup test 1 passed after aligning the smoke datasource with the module's existing H2 test dialect; green full `schemaplexai-workflow` module 210 passed.
- **2026-05-24 quality startup-smoke update**: `SchemaPlexaiQualityApplicationTest` now starts the real Quality application with `--server.port=0`, an H2 test datasource, Rabbit listener auto-start disabled, and a test-only `jwt.secret`. The module now has a test-scope H2 dependency for this startup path. `EscalationPolicyService.checkEscalations()` also has a configurable initial delay so startup smoke tests and fresh runtime starts do not immediately query approval tables before the application has settled. TDD evidence: red focused startup test failed on `Cannot load driver class: org.h2.Driver`; after adding H2, startup exposed an immediate scheduled-table query against an empty H2 database; red `EscalationPolicyServiceSchedulingTest#checkEscalationsDelaysFirstRunUntilStartupSettles` showed no `initialDelayString`; green scheduling contract 1 passed; green focused startup smoke 1 passed without the scheduled query error; green full `schemaplexai-quality` module 262 passed.
- **2026-05-24 ops startup-smoke update**: `SchemaPlexaiOpsApplicationTest` now starts the real Ops application with `--server.port=0`, an H2 test datasource, Rabbit listener auto-start disabled, `clickhouse.enabled=false`, and a test-only `jwt.secret`, asserting both the running Spring context and the disabled ClickHouse cost-sync fallback bean. The module now has a test-scope H2 dependency for this startup path. TDD evidence: red focused startup test failed on `Cannot load driver class: org.h2.Driver`; green focused startup smoke 1 passed after adding H2; green full `schemaplexai-ops` module 228 passed.
- **2026-05-24 agent-config startup-smoke update**: `SchemaPlexaiAgentConfigApplicationTest` now starts the real Agent Config application with `--server.port=0`, an H2 test datasource, Rabbit listener auto-start disabled, `clickhouse.enabled=false`, Flyway disabled, and a test-only `jwt.secret`, asserting the Spring context is running. The application scan is narrowed to `com.schemaplexai.agent.config` and `com.schemaplexai.dao`, mapper scanning includes config-local and DAO mappers, and `SecurityPolicyLoader` is imported explicitly for tenant environment policy refresh. TDD evidence: red focused startup test first failed on missing H2, then exposed a bean-definition conflict from broad `com.schemaplexai` scanning; green focused startup smoke 1 passed after narrowing scan boundaries; green full `schemaplexai-agent-config` reactor passed with 85 agent-config tests and 1823 agent-engine tests.
- **2026-05-24 admin startup-smoke update**: `SchemaPlexaiAdminApplicationTest` now starts the real Admin application with `--server.port=0`, an H2 test datasource, and a test-only `jwt.secret`, asserting the Spring context is running. The module now has a test-scope H2 dependency for this startup path. TDD evidence: red focused startup test failed on `Cannot load driver class: org.h2.Driver`; green focused startup smoke 1 passed after adding H2; green full `schemaplexai-admin` reactor passed with 111 admin tests and 128 system tests.
- **2026-05-21 system auth-login update**: `AuthService.login` now rejects blank tenant ids with `PARAM_ERROR` before user lookup, making the tenant requirement explicit at the authentication boundary.
- **2026-05-21 system logout update**: `AuthController.logout` now extracts the bearer token from the `Authorization` header and passes it to `AuthService.logout(userId, token)`, enabling the existing token blacklist path during logout.
- **2026-05-21 system tenant-policy update**: `TenantPolicyService.saveOrUpdatePolicy` now rejects blank tenant ids and blank policy types before insert/update or MQ publish, preventing malformed policy rows and invalid cache-invalidation events.
- **2026-05-24 system AuthServiceTest timing note**: Split AuthService refresh/logout/blacklist method groups all pass, and a fresh full `schemaplexai-system` module run passed 129 tests with full `AuthServiceTest` completing in 0.473s. The earlier very slow full-class run did not isolate to a stable slow method.
- **2026-05-24 integration Git webhook update**: `GitIntegrationService.handleWebhook` now rejects payloads whose repository resolves to blank or `unknown` before storing the event, preventing malformed webhook records from triggering downstream workflow/agent handling. TDD evidence: red `GitIntegrationServiceTest#handleWebhook_missingRepository_throwsParamErrorWithoutStoringEvent`; green focused test 1 passed; green full `GitIntegrationServiceTest` 43 passed; green full `schemaplexai-integration` module 268 passed.
- **2026-05-24 integration Jenkins trigger update**: `JenkinsIntegrationService.triggerBuild` now URL-encodes parameter names and values before posting `application/x-www-form-urlencoded` build requests, so branch names and manual causes containing `/` or spaces are sent safely. TDD evidence: red `JenkinsIntegrationServiceTest#triggerBuild_urlEncodesParameterNamesAndValues`; green focused test 1 passed; green full `JenkinsIntegrationServiceTest` 26 passed; green full `schemaplexai-integration` module 269 passed.
- **2026-05-24 integration webhook listing update**: `GitIntegrationService.listWebhookEvents` now short-circuits non-positive limits before scanning stored events, so `limit=0` returns an empty page even when matching webhook records exist. TDD evidence: red `GitIntegrationServiceTest#listWebhookEvents_zeroLimitWithStoredEvents_returnsEmpty`; green focused test 1 passed; green full `GitIntegrationServiceTest` 44 passed; green full `schemaplexai-integration` module 270 passed.
- **2026-05-24 quality approval RBAC update**: `ApprovalTicketService` now rejects approver ids with unsupported role prefixes instead of falling through to the raw-user fallback, preventing `AUDITOR:user-1`-style principals from approving tenant-owned tickets. TDD evidence: red `ApprovalTicketServiceTest#rbac_unknownRolePrefix_forbidden`; green focused test 1 passed; green full `ApprovalTicketServiceTest` 26 passed; green full `schemaplexai-quality` module 260 passed.
- **2026-05-24 context RAG tenant validation update**: `RagSearchServiceImpl.search` now validates invalid tenant ids before embedding or Milvus calls and surfaces `PARAM_ERROR` instead of wrapping the issue as an internal search failure. TDD evidence: red `RagSearchServiceImplTest#search_invalidTenantIdFormat_throwsParamErrorBeforeEmbedding`; green focused test 1 passed; green full `RagSearchServiceImplTest` 15 passed; green full `schemaplexai-context` module 193 passed.
- **2026-05-24 agent-config tenant environment boundary update**: `TenantEnvironmentConfigServiceImpl` now rejects blank tenant ids before querying tenant environment config or refreshing the security policy cache. TDD evidence: red `TenantEnvironmentConfigServiceImplTest#getByTenantId_blankTenantId_throwsParamErrorWithoutQuery`; red `TenantEnvironmentConfigServiceImplTest#refreshCache_blankTenantId_throwsParamErrorWithoutRefresh`; green focused tests 1 passed each; green full `TenantEnvironmentConfigServiceImplTest` 9 passed; green full `schemaplexai-agent-config` module 84 passed.
- **2026-05-24 spec publish/archive boundary update**: `SpecServiceImpl.publishSpec` and `archiveSpec` now reject `null` spec ids with `PARAM_ERROR` before mapper lookup, keeping invalid input distinct from missing specs. TDD evidence: red `SpecServiceImplTest#publishSpec_nullSpecId_throwsParamErrorWithoutLookup`; red `SpecServiceImplTest#archiveSpec_nullSpecId_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecServiceImplTest` 17 passed; green full `schemaplexai-spec` module 125 passed.
- **2026-05-24 spec version diff boundary update**: `SpecVersionServiceImpl.diff` now rejects `null` version ids with `PARAM_ERROR` before mapper lookup, so invalid diff requests are not reported as missing spec versions. TDD evidence: red `SpecVersionServiceImplTest#diff_nullVersionAId_throwsParamErrorWithoutLookup`; red `SpecVersionServiceImplTest#diff_nullVersionBId_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecVersionServiceImplTest` 8 passed; green full `schemaplexai-spec` module 127 passed.
- **2026-05-24 spec version create boundary update**: `SpecVersionServiceImpl.createVersion` now rejects a `null` spec id with `PARAM_ERROR` before spec lookup or version insert, preserving the distinction between invalid input and a missing spec. TDD evidence: red `SpecVersionServiceImplTest#createVersion_nullSpecId_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecVersionServiceImplTest` 9 passed; green full `schemaplexai-spec` module 128 passed.
- **2026-05-24 spec latest-version boundary update**: `SpecServiceImpl.getLatestVersion` now rejects a `null` spec id with `PARAM_ERROR` before querying version records, avoiding accidental null-condition lookups. TDD evidence: red `SpecServiceImplTest#getLatestVersion_nullSpecId_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecServiceImplTest` 18 passed; green full `schemaplexai-spec` module 129 passed.
- **2026-05-24 spec version-compare boundary update**: `SpecServiceImpl.compareVersions` now rejects `null` spec ids and blank version labels with `PARAM_ERROR` before querying version records, avoiding null or blank comparison filters. TDD evidence: red `SpecServiceImplTest#compareVersions_nullSpecId_throwsParamErrorWithoutLookup`; red `SpecServiceImplTest#compareVersions_blankVersionA_throwsParamErrorWithoutLookup`; red `SpecServiceImplTest#compareVersions_blankVersionB_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecServiceImplTest` 21 passed; green full `schemaplexai-spec` module 132 passed.
- **2026-05-24 spec template id boundary update**: `SpecTemplateServiceImpl.applyTemplate` and `cloneTemplate` now reject `null` template ids with `PARAM_ERROR` before template lookup, keeping invalid requests distinct from missing templates. TDD evidence: red `SpecTemplateServiceImplTest#applyTemplate_nullTemplateId_throwsParamErrorWithoutLookup`; red `SpecTemplateServiceImplTest#cloneTemplate_nullTemplateId_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecTemplateServiceImplTest` 13 passed; green full `schemaplexai-spec` module 134 passed.
- **2026-05-24 spec review spec-id boundary update**: `SpecReviewServiceImpl.submitReview` now rejects a `null` spec id with `PARAM_ERROR` before spec lookup or review insert, keeping invalid review requests distinct from missing specs. TDD evidence: red `SpecReviewServiceImplTest#submitReview_nullSpecId_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecReviewServiceImplTest` 7 passed; green full `schemaplexai-spec` module 135 passed.
- **2026-05-24 spec review reviewer boundary update**: `SpecReviewServiceImpl.submitReview` now rejects a `null` reviewer id with `PARAM_ERROR` before spec lookup or review insert, matching the `sf_spec_review.reviewer_id NOT NULL` persistence contract. TDD evidence: red `SpecReviewServiceImplTest#submitReview_nullReviewerId_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecReviewServiceImplTest` 8 passed; green full `schemaplexai-spec` module 136 passed.
- **2026-05-24 spec review status boundary update**: `SpecReviewServiceImpl.submitReview` now rejects blank review statuses with `PARAM_ERROR` before spec lookup or review insert, matching the `sf_spec_review.status NOT NULL` persistence contract. TDD evidence: red `SpecReviewServiceImplTest#submitReview_blankStatus_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecReviewServiceImplTest` 9 passed; green full `schemaplexai-spec` module 137 passed.
- **2026-05-24 spec template clone-name boundary update**: `SpecTemplateServiceImpl.cloneTemplate` now rejects blank clone names with `PARAM_ERROR` before source-template lookup or clone insert, matching the `sf_spec_template.name NOT NULL` persistence contract. TDD evidence: red `SpecTemplateServiceImplTest#cloneTemplate_blankNewName_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `SpecTemplateServiceImplTest` 14 passed; green full `schemaplexai-spec` module 138 passed.
- **2026-05-24 spec template apply-create boundary update**: `SpecTemplateServiceImpl.applyTemplate` now rejects blank titles and blank types before template lookup when creating a new spec from a template, matching the `sf_spec.title` and `sf_spec.type` NOT NULL persistence contract. TDD evidence: red `SpecTemplateServiceImplTest#applyTemplate_withoutSpecId_blankTitle_throwsParamErrorWithoutLookup`; red `SpecTemplateServiceImplTest#applyTemplate_withoutSpecId_blankType_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecTemplateServiceImplTest` 16 passed; green full `schemaplexai-spec` module 140 passed.
- **2026-05-24 spec create-from-template boundary update**: `SpecServiceImpl.createFromTemplate` now rejects `null` template ids, blank titles, and blank types with `PARAM_ERROR` before template lookup or spec insert, matching the `sf_spec.title` and `sf_spec.type` NOT NULL persistence contract. TDD evidence: red `SpecServiceImplTest#createFromTemplate_nullTemplateId_throwsParamErrorWithoutLookup`; red `SpecServiceImplTest#createFromTemplate_blankTitle_throwsParamErrorWithoutLookup`; red `SpecServiceImplTest#createFromTemplate_blankType_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecServiceImplTest` 24 passed; green full `schemaplexai-spec` module 143 passed.
- **2026-05-24 spec steering id boundary update**: `SpecSteeringServiceImpl` now rejects `null` spec ids for rule evaluation, steering application, and active-steering listing, and rejects `null` steering ids for config validation before mapper lookup. This keeps invalid steering requests distinct from missing records and matches the `sf_spec_steering.spec_id NOT NULL` persistence contract. TDD evidence: red `SpecSteeringServiceImplTest#evaluateSteeringRules_nullSpecId_throwsParamErrorWithoutLookup`; red `SpecSteeringServiceImplTest#applySteering_nullSpecId_throwsParamErrorWithoutLookup`; red `SpecSteeringServiceImplTest#listActiveSteerings_nullSpecId_throwsParamErrorWithoutLookup`; red `SpecSteeringServiceImplTest#validateSteeringConfig_nullSteeringId_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecSteeringServiceImplTest` 22 passed; green full `schemaplexai-spec` module 147 passed.
- **2026-05-24 spec version label boundary update**: `SpecVersionServiceImpl.createVersion` now rejects `null` and blank version labels with `PARAM_ERROR` before spec lookup or version insert, matching the `sf_spec_version.version NOT NULL` persistence contract and keeping invalid create-version requests distinct from missing specs. TDD evidence: red `SpecVersionServiceImplTest#createVersion_nullVersion_throwsParamErrorWithoutLookup`; red `SpecVersionServiceImplTest#createVersion_blankVersion_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `SpecVersionServiceImplTest` 11 passed; green full `schemaplexai-spec` module 149 passed.
- **2026-05-24 spec startup-smoke update**: `SchemaPlexaiSpecApplicationTest` now starts the real Spec application with `--server.port=0`, an H2 test datasource, and a test-only `jwt.secret`, asserting the context is running instead of relying only on controller/service unit coverage. The module now has a test-scope H2 dependency for this startup path. TDD evidence: red focused startup test failed on `Cannot load driver class: org.h2.Driver`; green focused startup test 1 passed after adding H2; green full `schemaplexai-spec` module 150 passed.
- **2026-05-24 context snapshot boundary update**: `ContextSnapshotServiceImpl` now rejects `null` snapshot JSON before context lookup or snapshot insert, and rejects `null` snapshot ids for restore/compare operations before mapper lookup. This keeps malformed snapshot requests distinct from missing contexts or missing snapshots and matches the `sf_context_snapshot.snapshot_json NOT NULL` persistence contract. TDD evidence: red `ContextSnapshotServiceImplTest#createSnapshot_nullSnapshotJson_throwsParamErrorWithoutLookup`; red `ContextSnapshotServiceImplTest#restoreFromSnapshot_nullSnapshotId_throwsParamErrorWithoutLookup`; red `ContextSnapshotServiceImplTest#compareSnapshots_nullSnapshotIdA_throwsParamErrorWithoutLookup`; red `ContextSnapshotServiceImplTest#compareSnapshots_nullSnapshotIdB_throwsParamErrorWithoutLookup`; green focused tests 1 passed each; green full `ContextSnapshotServiceImplTest` 23 passed; green full `schemaplexai-context` module 197 passed.
- **2026-05-24 context workspace tenant boundary update**: `WorkspaceServiceImpl.createDefaultWorkspace` now rejects `null` and blank tenant ids with `PARAM_ERROR` before workspace insert, preventing malformed default workspace rows that violate the `sf_workspace.tenant_id NOT NULL` multi-tenant persistence contract. TDD evidence: red `WorkspaceServiceImplTest#createDefaultWorkspace_nullTenantId_throwsParamErrorWithoutInsert`; red `WorkspaceServiceImplTest#createDefaultWorkspace_blankTenantId_throwsParamErrorWithoutInsert`; green focused tests 1 passed each; green full `WorkspaceServiceImplTest` 12 passed; green full `schemaplexai-context` module 198 passed.
- **2026-05-24 context ingest boundary update**: `ContextServiceImpl.ingestContext` now rejects `null` workspace ids, `null`/blank context types, and missing/blank tenant context with `PARAM_ERROR` before context insert. This prevents malformed context rows that violate the `sf_context.workspace_id`, `sf_context.type`, and `sf_context.tenant_id` `NOT NULL` multi-tenant persistence contract. TDD evidence: red `ContextServiceImplTest#ingestContext_missingTenantContext_throwsParamErrorWithoutInsert`; red `ContextServiceImplTest#ingestContext_nullWorkspaceId_throwsParamErrorWithoutInsert`; red `ContextServiceImplTest#ingestContext_blankTenantContext_throwsParamErrorWithoutInsert`; red `ContextServiceImplTest#ingestContext_nullType_throwsParamErrorWithoutInsert`; red `ContextServiceImplTest#ingestContext_blankType_throwsParamErrorWithoutInsert`; green focused tests 1 passed each; green full `ContextServiceImplTest` 23 passed; green full `schemaplexai-context` module 202 passed.
- **2026-05-24 context refresh boundary update**: `ContextServiceImpl.refreshContext` now rejects `null` context ids with `PARAM_ERROR` before mapper lookup, keeping malformed refresh requests distinct from missing contexts. TDD evidence: red `ContextServiceImplTest#refreshContext_nullId_throwsParamErrorWithoutLookup`; green focused test 1 passed; green full `ContextServiceImplTest` 24 passed; green full `schemaplexai-context` module 203 passed.
- **2026-05-24 context workspace access boundary update**: `WorkspaceServiceImpl` now rejects `null` workspace ids for access validation and archive operations, and rejects `null`/blank tenant ids for workspace listing before mapper lookup. This keeps malformed workspace requests distinct from missing workspaces and prevents blank tenant filters from reaching persistence. TDD evidence: red `WorkspaceServiceImplTest#validateWorkspaceAccess_nullWorkspaceId_throwsParamErrorWithoutLookup`; red `WorkspaceServiceImplTest#archiveWorkspace_nullWorkspaceId_throwsParamErrorWithoutLookup`; red `WorkspaceServiceImplTest#listWorkspacesByTenant_nullTenantId_throwsParamErrorWithoutQuery`; red `WorkspaceServiceImplTest#listWorkspacesByTenant_blankTenantId_throwsParamErrorWithoutQuery`; green focused tests 1 passed each; green full `WorkspaceServiceImplTest` 16 passed; green full `schemaplexai-context` module 207 passed.
- **2026-05-24 context Milvus sync doc-id boundary update**: `MilvusSyncServiceImpl` now rejects `null` doc ids for sync, vector delete, and re-sync operations with `PARAM_ERROR` before mapper lookup, Milvus collection lookup, or Milvus delete/insert calls. This prevents malformed async sync requests from being misreported as missing documents or surfacing as raw `NullPointerException`. TDD evidence: red `MilvusSyncServiceImplTest#syncToMilvus_nullDocId_throwsParamErrorWithoutLookup`; red `MilvusSyncServiceImplTest#deleteByDocId_nullDocId_throwsParamErrorWithoutMilvusCall`; red `MilvusSyncServiceImplTest#reSyncDoc_nullDocId_throwsParamErrorWithoutLookupOrMilvusCall`; green focused tests 1 passed each; green full `MilvusSyncServiceImplTest` 9 passed; green full `schemaplexai-context` module 210 passed.
- **2026-05-24 context knowledge-doc upload boundary update**: `KnowledgeDocServiceImpl.uploadAndVectorize` now rejects `null` docs, blank titles, and missing/blank tenant context with `PARAM_ERROR` before saving or triggering Milvus sync, and stamps the uploaded document with the current `TenantContextHolder` tenant. This keeps malformed knowledge documents from violating the `sf_knowledge_doc.title` and `sf_knowledge_doc.tenant_id` `NOT NULL` persistence contract and avoids trusting request-body tenant ids at the service boundary. TDD evidence: red `KnowledgeDocServiceImplTest#uploadAndVectorize_nullDoc_throwsParamErrorWithoutSaveOrSync`; red `KnowledgeDocServiceImplTest#uploadAndVectorize_nullTitle_throwsParamErrorWithoutSaveOrSync`; red `KnowledgeDocServiceImplTest#uploadAndVectorize_missingTenantContext_throwsParamErrorWithoutSaveOrSync`; green focused `KnowledgeDocServiceImplTest#uploadAndVectorize_*` 7 passed; green full `KnowledgeDocServiceImplTest` 12 passed; green full `schemaplexai-context` module 215 passed.
- **2026-05-24 context knowledge-doc mutation boundary update**: `KnowledgeDocServiceImpl` now rejects `null` knowledge-doc ids for deletion, and rejects `null` update payloads or update payloads without ids with `PARAM_ERROR` before Milvus cleanup, mapper deletion, mapper update, or re-sync calls. The delete path also covers MyBatis-Plus `removeById(T)` overload resolution for `null` and avoids the logic-delete overload recursion by delegating DB deletion through `super.removeById(id, false)` after Milvus cleanup. TDD evidence: red `KnowledgeDocServiceImplTest#removeById_nullId_throwsParamErrorWithoutMilvusOrDbRemoval`; red `KnowledgeDocServiceImplTest#updateById_nullEntity_throwsParamErrorWithoutUpdateOrReSync`; green focused delete/update boundary tests 3 passed; green full `KnowledgeDocServiceImplTest` 15 passed; green full `schemaplexai-context` module 218 passed.
- **2026-05-24 context failed-status writer boundary update**: `FailedStatusWriter.markFailed` now rejects `null` doc ids with `PARAM_ERROR` before mapper lookup, and marks both `status` and `syncStatus` as `FAILED` in the isolated failure transaction. This keeps malformed async failure writes from becoming missing-document warnings and keeps the knowledge-doc lifecycle status aligned with the vector-sync status after Milvus failures. TDD evidence: red `FailedStatusWriterTest#markFailed_nullDocId_throwsParamErrorWithoutLookupOrUpdate`; red `FailedStatusWriterTest#markFailed_existingDoc_updatesStatusAndSyncStatus`; green focused sync-status test 1 passed; green full `FailedStatusWriterTest` 3 passed; green full `schemaplexai-context` module 219 passed.
- **2026-05-24 context Milvus sync consumer boundary update**: `MilvusSyncConsumer.consume` now rejects `null` doc ids with `PARAM_ERROR` before delegating to `MilvusSyncService`, keeping malformed queue payloads at the MQ boundary instead of relying on downstream service validation or no-op implementations. TDD evidence: red `MilvusSyncConsumerTest#consume_nullDocId_throwsParamErrorWithoutSync`; green focused test 1 passed; green full `MilvusSyncConsumerTest` 2 passed; green full `schemaplexai-context` module 220 passed.
- **2026-05-24 context no-op Milvus service boundary update**: `NoOpMilvusSyncServiceImpl` now rejects `null` doc ids for sync, vector delete, and re-sync operations with `PARAM_ERROR`, matching the real `MilvusSyncServiceImpl` contract even when Milvus is disabled. This prevents disabled-Milvus deployments from silently accepting malformed document ids that enabled-Milvus deployments reject. TDD evidence: red `NoOpMilvusSyncServiceImplTest#syncToMilvus_nullDocId_throwsParamError`; red `NoOpMilvusSyncServiceImplTest#deleteByDocId_nullDocId_throwsParamError`; red `NoOpMilvusSyncServiceImplTest#reSyncDoc_nullDocId_throwsParamError`; green focused tests 1 passed each; green full `NoOpMilvusSyncServiceImplTest` 4 passed; green full `schemaplexai-context` module 223 passed.
- **2026-05-24 context file-storage fallback update**: `DisabledFileStorageService` now provides a default `FileStorageService` bean when MinIO is disabled or absent, so `FileUploadController` no longer breaks context assembly solely because object storage is not enabled. The fallback rejects uploads with `BaseException(INTERNAL_ERROR)` instead of returning fake URLs or throwing a raw `UnsupportedOperationException`. TDD evidence: red `FileStorageServiceBeanTest#minioDisabled_stillProvidesFileStorageServiceBean`; red `DisabledFileStorageServiceTest#upload_throwsInternalErrorWhenStorageIsDisabled`; red `FileStorageServiceBeanTest#minioMissing_componentScanProvidesDisabledStorageBean` reproduced the real component-scan gap; green focused storage fallback group 3 passed; focused app startup then advanced past `FileStorageService` to the next missing `MilvusSyncService` bean; green full `schemaplexai-context` module 227 passed after the companion fallback and startup-smoke fixes.
- **2026-05-24 context Milvus fallback bean update**: `NoOpMilvusSyncServiceImpl` now registers explicitly when `milvus.enabled=false` or missing, matching the real `MilvusSyncServiceImpl` `milvus.enabled=true` condition and preventing `KnowledgeDocServiceImpl` assembly failures in disabled-Milvus deployments. TDD evidence: red `MilvusSyncServiceBeanTest#milvusMissing_componentScanProvidesNoOpMilvusSyncServiceBean`; green focused test 1 passed; green fallback group `MilvusSyncServiceBeanTest,NoOpMilvusSyncServiceImplTest,FileStorageServiceBeanTest,DisabledFileStorageServiceTest` 8 passed; focused app startup then advanced past storage/Milvus bean assembly.
- **2026-05-24 context startup-smoke update**: `SchemaPlexaiContextApplicationMainTest` no longer swallows startup exceptions. It now starts the real application with `--server.port=0` and a test-only `jwt.secret`, asserts the context is running, and closes it. TDD evidence: red `SchemaPlexaiContextApplicationMainTest#applicationStartsWithTestConfiguration` failed on `JWT secret is not configured`; green focused startup test 1 passed with `JWT secret validated successfully (43 bytes)`; green full `schemaplexai-context` module 227 passed.
- **2026-05-24 context startup-smoke cleanup**: Removed the redundant `SchemaPlexaiContextApplicationTest` that only instantiated `new SchemaPlexaiContextApplication()` and could not catch startup failures. The real startup guard remains `SchemaPlexaiContextApplicationMainTest`, and no remaining checked startup tests in context/gateway/integration/system/task/web use the instantiation-only pattern. Verification evidence: focused `SchemaPlexaiContextApplicationMainTest` 1 passed; green full context reactor `schemaplexai-context` 226 passed, 0 failures, 0 errors.
- **2026-05-24 gateway startup-smoke update**: `GatewayApplicationTest` no longer swallows startup exceptions. It now starts the real Gateway application with `--server.port=0` and a test-only `jwt.secret`, asserts the context is running, and closes it. The Gateway module now explicitly sets `spring.main.web-application-type=reactive`, preventing the common module's transitive Spring MVC classpath from tripping Spring Cloud Gateway startup. TDD evidence: red focused startup test first failed on unresolved `${JWT_SECRET}`, then after adding a test secret failed on `Spring MVC found on classpath`; green focused startup test 1 passed after the reactive application-type fix; green full `schemaplexai-gateway` module 34 passed.
- **2026-05-24 system startup-smoke update**: `SchemaPlexaiSystemApplicationTest` no longer only asserts that the application class exists. It now starts the real System application with `--server.port=0` and a test-only `jwt.secret`, asserts the context is running, and closes it. TDD evidence: red focused startup test failed on unresolved `${JWT_SECRET}` through `JwtTokenProvider`; green focused startup test 1 passed with `JWT secret validated successfully (42 bytes)`; green full `schemaplexai-system` module 128 passed.
- **2026-05-24 integration startup-smoke update**: `SchemaPlexaiIntegrationApplicationTest` no longer avoids `SpringApplication.run`. It now starts the real Integration application with `--server.port=0`, a test-only H2 datasource, and a test-only `jwt.secret`, asserts the context is running, and closes it. The module now has an H2 test dependency for this smoke path. TDD evidence: red focused startup test first failed on missing datasource driver/url, then after adding H2 datasource failed on missing JWT secret; green focused startup test 1 passed with `JWT secret validated successfully (47 bytes)`; green full `schemaplexai-integration` module 270 passed.
- **2026-05-24 task startup-smoke update**: `SchemaPlexaiTaskApplicationTest` now starts the real Task application with H2, disables Rabbit listener auto-start, asserts `LockProvider` exists, and verifies the `shedlock` table is available. `ShedLockConfig` provides a JDBC `LockProvider`, task startup no longer scans every `com.schemaplexai` service package, and task-owned consumers/jobs that need cross-module runtime beans are guarded with `@ConditionalOnBean`. Flyway is disabled for the task runtime to avoid executing dependency-module migrations in this scheduler/consumer layer. `HealthCheckJob` now has a configurable initial delay so startup smoke tests do not immediately probe local Redis/RabbitMQ. TDD evidence: red startup test exposed duplicate bean scanning, missing cross-module beans, Flyway dependency migration leakage, missing `LockProvider`, then missing `shedlock`; red health-check contract test showed no `initialDelayString`; green focused startup smoke 1 passed; green health-check schedule test 1 passed; green full task reactor `schemaplexai-task` 144 passed.
- **2026-05-25 agent-engine event mapping cleanup**: `ExecutionEvent.eventId` is now the explicit MyBatis-Plus primary key for `sf_execution_event.event_id`; `ProcessedEventMapper` no longer extends single-key `BaseMapperX` because `sf_processed_event` uses composite key `(event_id, consumer_name)`. A local `UuidTypeHandler` binds annotated composite-key SQL parameters as JDBC `OTHER`, fixing the startup failure exposed after removing the fake single-key mapper. TDD evidence: red `EventEntityMappingTest` first exposed missing MP table metadata and `BaseMapperX` exposure; red `SchemaPlexaiAgentEngineApplicationTest` then exposed the UUID TypeHandler mapper startup failure; red `UuidTypeHandlerTest#setNonNullParameterBindsUuidAsJdbcOther` exposed two-arg UUID binding; green combined focused run `EventEntityMappingTest,UuidTypeHandlerTest,SchemaPlexaiAgentEngineApplicationTest` passed 7 tests with 0 failures and no prior `ExecutionEvent` / `ProcessedEvent` `Not found @TableId` warnings in startup logs.
- **2026-05-25 agent-engine exploration runtime update**: `AgentLab` no longer returns deterministic simulated experiment results when no experiment runtime is configured; default Spring construction now fails explicitly until a real runtime is supplied, while tests inject a fake runtime for result comparison. `ResearchAutomation` also no longer fabricates `example.com` search sources and requires an injected search runtime for real discovery. TDD evidence: red `AgentLabTest#runExperiment_withoutRuntime_throwsInsteadOfSimulatingResults` in the focused `AgentLabTest` run failed with 15 tests run, 1 failure, 0 errors because the old implementation returned simulated results; red `ResearchAutomationTest#researchTopic_withoutSearchRuntime_throwsInsteadOfReturningExampleSources` failed in the implementer run because the old implementation returned example sources; green focused `AgentLabTest` 15 passed; green focused `ResearchAutomationTest` 15 passed; green combined `AgentLabTest,ResearchAutomationTest` 30 passed; green full `rtk mvn test -pl schemaplexai-agent-engine -am` reactor passed with `schemaplexai-agent-engine` 1843 run, 0 failures, 0 errors, 4 skipped. Note: `rtk mvn test -pl schemaplexai-agent-engine` without `-am` failed during `SchemaPlexaiAgentEngineApplicationTest` because the local `.m2` `schemaplexai-common` SNAPSHOT lacked the current observability fallback auto-configuration; the `-am` reactor run verified the current workspace source chain.
- **2026-05-25 task cost-sync ACK semantics update**: `CostSyncConsumer` no longer ACKs unsupported full/range cost-sync requests as successful incremental sync. It validates `forceFullSync`, nonblank `dateRange`, and unsupported `syncType` before delegating to `CostDataSyncService`, nacks/logs unsupported requests, and only ACKs after the supported incremental/api path runs. The consumer and fallback configuration tests now use the `CostDataSyncService` interface directly, with the stale Redis constructor dependency removed from this path. TDD evidence: red focused `CostSyncConsumerTest` with `-am` ran 7 tests, 2 failures, 0 errors because full/range requests were ACKed and `syncIncrementalData()` was called; green focused `CostSyncConsumerTest,CostSyncDependencyConfigurationTest` passed 9 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 147 run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 ops/task cost projection failure update**: `CostService.processExecutionEvent` no longer swallows persistence or budget-update failures for `TOKEN_USED` and `TOOL_CALL` events. Insert and budget-update exceptions now propagate to the caller, allowing `ExecutionEventConsumer` to nack and fail-log the message instead of ACKing a failed cost projection as successful processing. TDD evidence: red focused `CostServiceTest` ran 40 tests, 3 failures, 0 errors because insert/budget failures were swallowed; green focused `rtk mvn test -pl schemaplexai-ops -am "-Dtest=CostServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 40 tests, 0 failures, 0 errors; green consumer boundary `rtk mvn test -pl schemaplexai-task -am "-Dtest=ExecutionEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` passed 4 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-ops -am` reactor passed with 231 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task agent-execute dedup ACK semantics update**: `AgentExecuteDispatcher` no longer swallows `InboxDeduplicationService.markProcessed` failures after `AgentExecutionEngine.startExecution`. A dedup mark failure now flows into the existing fail-log + `basicNack(..., false, false)` path before dispatch success is logged or ACKed. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=AgentExecuteDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack`; green focused run passed 6 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 148 run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task cost-sync dedup ACK semantics update**: `CostSyncConsumer` no longer swallows `InboxDeduplicationService.markProcessed` failures after `CostDataSyncService.syncIncrementalData`. A dedup mark failure now flows into the existing fail-log + `basicNack(..., false, false)` path before cost-sync success is logged or ACKed. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 9 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack`; green focused run passed 9 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 149 run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task notification dedup ACK semantics update**: `NotificationConsumer` no longer swallows `InboxDeduplicationService.markProcessed` failures after in-app notification persistence. A dedup mark failure now flows into the existing fail-log + `basicNack(..., false, false)` path before delivery success is logged or ACKed. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` and never called `basicNack`; green focused run passed 10 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 150 run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 quality approval-request dedup transaction update**: `ApprovalRequestConsumer` no longer swallows `InboxDeduplicationService.markProcessed` failures after approval-ticket insert, and `consume(...)` is now transactional so the ticket insert and processed-event mark succeed or fail together. TDD evidence: red focused `rtk mvn test -pl schemaplexai-quality -am "-Dtest=ApprovalRequestIdempotencyTest" "-Dsurefire.failIfNoSpecifiedTests=false"` first ran 4 tests, 1 failure, 0 errors because the old implementation swallowed `dedup mark failed`; green focused run passed 4 tests, 0 failures, 0 errors after propagating the failure; red focused transaction-boundary run then ran 5 tests, 1 failure, 0 errors because `consume(...)` lacked `@Transactional`; green focused run passed 5 tests, 0 failures, 0 errors after adding the boundary; green full `rtk mvn test -pl schemaplexai-quality -am` reactor passed with `schemaplexai-quality` 271 tests, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 agent-engine event-reordering ACK semantics update**: `EventReorderingConsumer` no longer swallows non-idempotent `ExecutionEventService.writeEvent(...)` failures for ready events before ACKing the Rabbit message. Duplicate-key persistence remains an idempotent ACK-compatible skip, but any other ready-event persistence failure now propagates to the outer handler and produces `basicNack(..., false, false)` instead of reporting successful processing. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=EventReorderingConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` after `writeEvent` failed; green focused run passed 6 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-agent-engine -am` reactor passed with `schemaplexai-agent-engine` 1844 tests run, 0 failures, 0 errors, 4 skipped.
- **2026-05-25 task cost-statistics scheduler failure update**: `CostStatisticsJob` no longer swallows `CostDataSyncService.syncIncrementalData()` or `CostService.checkBudgetAlerts()` failures as a successful scheduled run. The job still logs the failure, then rethrows runtime failures so scheduler/error handling and tests can observe the failed side effect instead of treating cost statistics as completed. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostStatisticsJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 3 tests, 2 failures, 0 errors because the old implementation logged and swallowed `sync failed` and `budget alert failed`; green focused run passed 3 tests, 0 failures, 0 errors after failures propagated; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 151 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task memory-consolidation scheduler failure update**: `MemoryConsolidationJob` no longer swallows Redis `keys(...)` or `expire(...)` failures as a successful scheduled run. The job still logs the failure, then rethrows runtime failures so scheduler/error handling and tests can observe that memory TTL refresh did not complete. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=MemoryConsolidationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 2 failures, 0 errors because the old implementation logged and swallowed `Redis error` and `expire failed`; green focused run passed 5 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 152 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task approval-timeout scheduler failure update**: `ApprovalTimeoutJob` no longer swallows `EscalationPolicyService.checkEscalations()` failures as a successful scheduled run. The job still logs the failure, then rethrows runtime failures so scheduler/error handling and tests can observe that approval escalation checks did not complete. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=ApprovalTimeoutJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `escalation failed`; green focused run passed 2 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 153 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task chat-message-archive scheduler failure update**: `ChatMessageArchiveJob` no longer swallows `ChatMessageArchiveService.archiveExpiredMessages(...)` failures as a successful scheduled run. The job still logs the failure, then rethrows runtime failures so scheduler/error handling and tests can observe that archive work did not complete. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=ChatMessageArchiveJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `archive failed`; green focused run passed 2 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 154 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task Milvus-reconciliation scheduler failure update**: `MilvusReconciliationJob` no longer swallows `MilvusReconciliationService.reconcilePendingDocuments(...)` failures as a successful scheduled run. The job still logs the failure, then rethrows runtime failures so scheduler/error handling and tests can observe that vector reconciliation did not complete. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusReconciliationJobTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 1 failure, 0 errors because the old implementation logged and swallowed `reconciliation failed`; green focused run passed 2 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 155 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task dead-letter audit failure update**: `DeadLetterHandler` no longer ACKs a parsed dead-letter message when publishing the required audit event fails. Invalid dead-letter payloads still ACK intentionally to avoid poison-message loops, but audit publication failures now produce `basicNack(..., false, false)` so the failed audit handoff is observable instead of being treated as completed dead-letter processing. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=DeadLetterHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 4 tests, 1 failure, 0 errors because the old implementation called `basicAck(1L, false)` after `MQ down`; green focused run passed 4 tests, 0 failures, 0 errors; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 155 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task MQ idempotency duplicate ACK update**: `MqIdempotencyInterceptor` now explicitly ACKs duplicate messages when a manual-ACK Rabbit `Channel` is present and the duplicate is detected through either Redis or the idempotency DB unique constraint. The duplicate paths still skip the business listener, the DB-duplicate path still writes the Redis idempotency cache, and non-Channel invocation keeps the previous return-null behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=MqIdempotencyInterceptorTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 8 tests, 2 failures, 0 errors because the old implementation never called `channel.basicAck(1L, false)` on Redis/DB duplicate skips; green focused run passed 8 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 157 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task message-fail-log persistence signal update**: `MessageFailLogService.log(...)` now returns `true` only when the fail-log insert affects at least one row. Database exceptions and zero-row inserts are logged and return `false`, making fail-log persistence loss observable to callers without breaking existing consumers that ignore the return value and continue their original NACK path. Existing entity field mapping and Unicode payload preservation remain covered. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=MessageFailLogServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 3 failures, 0 errors because the old `void` method returned `null`; green focused run passed 5 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 159 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task agent-execute fail-log observability update**: `AgentExecuteDispatcher` now checks the boolean result from `MessageFailLogService.log(...)` on dispatch failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=AgentExecuteDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 7 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 160 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task notification fail-log observability update**: `NotificationConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on unsupported-channel and exception failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=NotificationConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 11 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 11 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 161 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task workflow-trigger fail-log observability update**: `WorkflowTriggerConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on business and parsing failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=WorkflowTriggerConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 6 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 162 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task quality-event fail-log observability update**: `QualityEventConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on business and parsing failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=QualityEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 6 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 6 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 163 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task milvus-sync fail-log observability update**: `MilvusSyncConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on business and parsing failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=MilvusSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 7 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 164 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-25 task cost-sync fail-log observability update**: `CostSyncConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on exception failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=CostSyncConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 10 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-task -am` reactor passed with `schemaplexai-task` 165 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 task execution-event fail-log observability update**: `ExecutionEventConsumer` now checks the boolean result from `MessageFailLogService.log(...)` on execution-event parse and processing failure paths and emits a warning when fail-log persistence returns `false`, while preserving the existing `basicNack(..., false, false)` behavior. TDD evidence: red focused `rtk mvn test -pl schemaplexai-task -am "-Dtest=ExecutionEventConsumerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 1 failure, 0 errors because the old implementation produced no warning for a failed fail-log insert; green focused run passed 5 tests, 0 failures, 0 errors, 0 skipped. Broad affected-reactor note: the first `rtk mvn test -pl schemaplexai-task -am` run was blocked outside the touched scope by `AgentEngineBenchmarkTest.compositeBenchmarkSummary`, where `LoopDetection.detect` throughput dropped to `46623 < 50000` because the benchmark repeatedly triggered loop-warning logging. After switching the benchmark input to unique tool sequences, focused `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=AgentEngineBenchmarkTest#compositeBenchmarkSummary" "-Dsurefire.failIfNoSpecifiedTests=false"` passed, and the rerun broad gate `rtk mvn test -pl schemaplexai-task -am` completed successfully with `schemaplexai-task` 166 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agent-engine task-tool async false-success update**: `TaskToolAdapter` no longer reports a successful tool result when `SubAgentExecutionService` only returns the placeholder `"Sub-agent execution started..."` message from the async dispatch path. It now converts that placeholder into an explicit error telling operators that synchronous task-tool completion is not implemented yet, while preserving quota enforcement, guardrail propagation, and true completed sub-agent outputs. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-engine -am "-Dtest=TaskToolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 12 tests, 1 failure, 0 errors because the old adapter returned success for the async-start placeholder; green focused run passed 12 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-agent-engine -am` reactor passed with `schemaplexai-agent-engine` 1845 tests run, 0 failures, 0 errors, 4 skipped.
- **2026-05-26 agent-config zero-row write false-success update**: `AgentConfigService.updateAgent(...)` and `deleteAgent(...)` no longer silently report success when MyBatis updates or deletes zero rows. They now throw `BaseException(ResultCode.AGENT_NOT_FOUND)` when the target agent does not exist, preventing the controller layer from returning `Result.success()` for no-op writes that changed nothing. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 16 tests, 2 failures, 0 errors because the old service did not throw when `updateById`/`deleteById` returned `0`; green focused run passed 16 tests, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed with `schemaplexai-agent-config` 87 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agent-config saveAgentConfig update-path false-success update**: `AgentConfigService.saveAgentConfig(...)` no longer silently reports success when its update path calls `agentConfigMapper.updateById(config)` and MyBatis reports `0` affected rows. The update branch now throws `BaseException(ResultCode.AGENT_NOT_FOUND)`, preventing upstream callers from treating a missing agent-config row as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenUpdateAgentConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `saveAgentConfig(...)` update path ignored the zero-row result; green focused run passed 1 test, 0 failures, 0 errors, 0 skipped; green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 88 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agent-config insert-path false-success update**: `AgentConfigService.createAgent(...)` and the insert branch of `saveAgentConfig(...)` no longer silently report success when MyBatis `insert(...)` returns `0`. Both paths now throw `BaseException(ResultCode.INTERNAL_ERROR)`, preventing upstream callers from treating zero-row inserts as successful writes when no agent or agent-config row was actually persisted. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenCreateAgentAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `createAgent(...)` path ignored the zero-row insert result; green focused run later passed together with the save-config insert case via `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenInsertAgentConfigAffectsNoRows,AgentConfigServiceTest#shouldThrowWhenCreateAgentAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` with 2 tests, 0 failures, 0 errors, 0 skipped. Additional red evidence: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenInsertAgentConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `saveAgentConfig(...)` insert branch ignored the zero-row insert result. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 90 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agent-config tool-binding insert false-success update**: `AgentConfigService.saveToolBindings(...)` no longer silently reports success when `toolBindingMapper.insert(binding)` returns `0` for a binding row. The method now throws `BaseException(ResultCode.INTERNAL_ERROR)` instead of treating a zero-row tool-binding insert as a successful persistence operation. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentConfigServiceTest#shouldThrowWhenSaveToolBindingInsertAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old implementation ignored the zero-row insert result; green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service threw `INTERNAL_ERROR`; green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 91 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 shadow-config zero-row write false-success update**: `ShadowConfigService.createShadowConfig(...)`, `updateShadowConfig(...)`, and `deleteShadowConfig(...)` no longer silently report success when the mapper returns zero affected rows. `createShadowConfig(...)` now throws `BaseException(ResultCode.INTERNAL_ERROR)` for zero-row inserts, while `updateShadowConfig(...)` and `deleteShadowConfig(...)` throw `BaseException(ResultCode.NOT_FOUND)` when no shadow-config row is actually changed or removed. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=ShadowConfigServiceTest#shouldThrowWhenCreateShadowConfigAffectsNoRows+shouldThrowWhenUpdateShadowConfigAffectsNoRows+shouldThrowWhenDeleteShadowConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 3 tests, 3 failures, 0 errors because the old service ignored zero-row create/update/delete results; green focused run passed 3 tests, 0 failures, 0 errors, 0 skipped after the service checked mapper write counts. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 94 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 prompt-version insert false-success update**: `PromptVersionServiceImpl.createVersion(...)` no longer silently reports success when `promptVersionMapper.insert(pv)` returns `0`. The method now throws `IllegalStateException` with a clear message instead of returning a version object that was never persisted. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=PromptVersionServiceTest#shouldThrowWhenCreateVersionInsertAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old implementation ignored the zero-row insert result; green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert count and threw. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 95 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agents-manifest-loader zero-row write false-success update**: `AgentsManifestLoader` no longer silently reports manifest import success when agent create/update, agent-config create/update, or tool-binding insert operations affect zero rows. `upsertAgent(...)`, `upsertConfig(...)`, and `replaceToolBindings(...)` now throw explicit `IllegalStateException`s so `loadFromManifest(...)` cannot return an `agentId` for writes that never persisted. TDD evidence: earlier focused red runs reproduced the three create-path false-success gaps in `AgentsManifestLoaderTest` before the current guards existed: `shouldThrowWhenCreatingAgentAffectsNoRows`, `shouldThrowWhenCreatingAgentConfigAffectsNoRows`, and `shouldThrowWhenCreatingToolBindingAffectsNoRows`. Green focused update-path repair: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentsManifestLoaderTest#shouldThrowWhenUpdatingExistingAgentAffectsNoRows+shouldThrowWhenUpdatingExistingConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 2 tests, 0 failures, 0 errors, 0 skipped after removing obsolete stubs and keeping the new zero-row update guards. Green focused full slice: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentsManifestLoaderTest#shouldThrowWhenCreatingAgentAffectsNoRows+shouldThrowWhenCreatingAgentConfigAffectsNoRows+shouldThrowWhenCreatingToolBindingAffectsNoRows+shouldThrowWhenUpdatingExistingAgentAffectsNoRows+shouldThrowWhenUpdatingExistingConfigAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 5 tests, 0 failures, 0 errors, 0 skipped. Green focused full-class regression: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentsManifestLoaderTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 22 tests, 0 failures, 0 errors, 0 skipped after backfilling pre-existing discovery/config tests with the config/tool-binding stubs required by the new guards. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 100 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 agent-shadow toggle zero-row false-success update**: `AgentShadowConfigServiceImpl.toggleEnabled(...)` no longer silently reports success when it finds a shadow-config row by id but `updateById(config)` changes zero rows. The method now throws `BaseException(ResultCode.NOT_FOUND)` instead of treating a no-op toggle as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentShadowConfigServiceImplTest#shouldThrowNotFoundWhenToggleEnabledAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `toggleEnabled(...)` path did not throw when `updateById(...)` returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the update result and threw `NOT_FOUND`. Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=AgentShadowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 7 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 101 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 tenant-environment update zero-row false-success update**: `TenantEnvironmentConfigServiceImpl.updateById(...)` no longer silently reports success when it finds the tenant-environment row by id but `super.updateById(entity)` changes zero rows. The method now throws `BaseException(ResultCode.NOT_FOUND)` instead of treating a no-op update as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest#shouldThrowNotFoundWhenUpdateAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when the update returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the update result and threw `NOT_FOUND`. Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 10 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 102 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 tenant-environment create zero-row false-success update**: `TenantEnvironmentConfigServiceImpl.save(...)` no longer silently reports success when `super.save(entity)` changes zero rows. After validating `tenantId`, the method now throws `BaseException(ResultCode.INTERNAL_ERROR)` instead of treating a zero-row tenant-environment insert as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest#shouldThrowInternalErrorWhenCreateAffectsNoRows" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when the insert returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert result and threw `INTERNAL_ERROR`. Green focused full class: `rtk mvn test -pl schemaplexai-agent-config -am "-Dtest=TenantEnvironmentConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 11 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-agent-config -am` reactor passed common/model/dao/integration/ops/agent-engine/agent-config, with `schemaplexai-agent-config` 103 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 quality-gate create zero-row false-success update**: `QualityGateServiceImpl.save(...)` no longer silently reports success when `super.save(gate)` changes zero rows. After validation and default-status setup, the method now throws `BaseException(ResultCode.INTERNAL_ERROR)` instead of treating a zero-row quality-gate insert as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest#save_whenInsertAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when the insert returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert result and threw `INTERNAL_ERROR`. Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 20 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-quality -am` reactor passed common/model/dao/quality, with `schemaplexai-quality` 272 tests run, 0 failures, 0 errors, 0 skipped.
- **Next priority**: continue the next false-success candidate after agent-config zero-row write handling, keeping the same red/focused-green/broad-verification/documentation cadence and a maximum of 2 parallel subagents.
- **2026-05-26 quality-gate update zero-row false-success update**: `QualityGateServiceImpl.updateById(...)` no longer silently reports success when it finds the quality-gate row by id but `super.updateById(gate)` changes zero rows. The method now throws `BaseException(ResultCode.NOT_FOUND)` instead of treating a no-op quality-gate update as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest#updateById_whenUpdateAffectsNoRows_throwsNotFound" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when the update returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the update result and threw `NOT_FOUND`. Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=QualityGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 21 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-quality -am` reactor passed common/model/dao/quality, with `schemaplexai-quality` 273 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 user register zero-row false-success update**: `UserService.register(...)` no longer silently reports success when `save(user)` changes zero rows. After username uniqueness validation, password encoding, and default-status setup, the method now throws `BaseException(ResultCode.INTERNAL_ERROR)` instead of returning a successful registration result for a zero-row user insert. TDD evidence: red focused `rtk mvn test -pl schemaplexai-system -am "-Dtest=UserServiceTest#register_whenSaveAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `register(...)` path did not throw when the insert returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert result and threw `INTERNAL_ERROR`. Green focused full class: `rtk mvn test -pl schemaplexai-system -am "-Dtest=UserServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 12 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-system -am` reactor passed common/model/dao/system, with `schemaplexai-system` 129 tests run, 0 failures, 0 errors, 0 skipped.
- **2026-05-26 quality security-policy create zero-row false-success update**: `SecurityPolicyServiceImpl.save(...)` no longer silently reports success when `super.save(policy)` changes zero rows. After validation, default-status setup, and logging, the method now throws `BaseException(ResultCode.INTERNAL_ERROR)` instead of treating a zero-row security-policy insert as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest#save_whenInsertAffectsNoRows_throwsInternalError" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `save(...)` path did not throw when the insert returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the insert result and threw `INTERNAL_ERROR`. Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 27 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-quality -am` reactor passed common/model/dao/quality, with `schemaplexai-quality` 275 tests run, 0 failures, 0 errors, 0 skipped. During this slice, UTF-8 BOM bytes at the heads of `QualityGateServiceImpl.java` and `QualityGateServiceImplTest.java` were also removed so the quality reactor could compile again; that encoding cleanup did not change business behavior.
- **2026-05-26 quality security-policy update zero-row false-success update**: `SecurityPolicyServiceImpl.updateById(...)` no longer silently reports success when it finds the security-policy row by id but `super.updateById(policy)` changes zero rows. The method now throws `BaseException(ResultCode.NOT_FOUND)` instead of treating a no-op security-policy update as a successful write. TDD evidence: red focused `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest#updateById_whenUpdateAffectsNoRows_throwsNotFound" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 1 test, 1 failure, 0 errors because the old `updateById(...)` path did not throw when the update returned zero rows. Green focused run passed 1 test, 0 failures, 0 errors, 0 skipped after the service checked the update result and threw `NOT_FOUND`. Green focused full class: `rtk mvn test -pl schemaplexai-quality -am "-Dtest=SecurityPolicyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"` ran 27 tests, 0 failures, 0 errors, 0 skipped. Green full `rtk mvn test -pl schemaplexai-quality -am` reactor passed common/model/dao/quality, with `schemaplexai-quality` 275 tests run, 0 failures, 0 errors, 0 skipped.
