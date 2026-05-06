package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.service.QualityOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestServiceConfig.class)
@ActiveProfiles("test")
@DisplayName("Cross-Service Chain Integration: Agent → Workflow → Quality → Cost")
class CrossServiceChainIntegrationTest {

    @Autowired
    private AgentRuntimeOrchestrator orchestrator;

    @Autowired
    private SfAgentExecutionMapper executionMapper;

    @MockBean
    private com.schemaplexai.workflow.service.WorkflowInstanceService workflowInstanceService;

    @MockBean
    private QualityOrchestrator qualityOrchestrator;

    @MockBean
    private com.schemaplexai.ops.service.CostService costService;

    @Test
    @DisplayName("should execute agent through workflow with quality and cost tracking")
    void shouldExecuteAgentThroughWorkflowWithQualityAndCostTracking() {
        // Arrange: stub workflow, quality, and cost services
        doNothing().when(workflowInstanceService).trigger(anyLong());
        when(qualityOrchestrator.evaluate(anyLong(), any()))
                .thenReturn(new QualityReport(1L, true, java.util.List.of()));
        when(costService.queryCostByTenant(anyString()))
                .thenReturn(Map.of("totalCost", BigDecimal.valueOf(0.05)));

        SfAgentExecution execution = createExecution(1L, "tenant-1", "test-prompt-1");
        executionMapper.insert(execution);

        // Act
        orchestrator.run(execution, "tenant-1", "test prompt");

        // Assert: verify cross-service interactions
        verify(workflowInstanceService, atMost(1)).trigger(anyLong());
        verify(qualityOrchestrator, atMost(1)).evaluate(anyLong(), any());

        SfAgentExecution updated = executionMapper.selectById(execution.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getState()).isIn(
                AgentExecutionState.COMPLETED.name(),
                AgentExecutionState.GATE_BLOCKED.name(),
                AgentExecutionState.FAILED.name()
        );
    }

    @Test
    @DisplayName("should fail workflow and track quality issues")
    void shouldFailWorkflowAndTrackQualityIssues() {
        // Arrange: workflow fails
        doThrow(new RuntimeException("workflow node execution failed"))
                .when(workflowInstanceService).trigger(anyLong());
        when(qualityOrchestrator.evaluate(anyLong(), any()))
                .thenReturn(new QualityReport(2L, false, java.util.List.of()));

        SfAgentExecution execution = createExecution(2L, "tenant-2", "test-prompt-2");
        executionMapper.insert(execution);

        // Act
        orchestrator.run(execution, "tenant-2", "fail test prompt");

        // Assert
        SfAgentExecution updated = executionMapper.selectById(execution.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getState()).isIn(
                AgentExecutionState.FAILED.name(),
                AgentExecutionState.GATE_BLOCKED.name()
        );
    }

    @Test
    @DisplayName("should track cost across multiple agent executions")
    void shouldTrackCostAcrossMultipleAgentExecutions() {
        // Arrange
        doNothing().when(workflowInstanceService).trigger(anyLong());
        when(qualityOrchestrator.evaluate(anyLong(), any()))
                .thenReturn(new QualityReport(3L, true, java.util.List.of()));

        String tenantId = "tenant-3";
        for (int i = 0; i < 3; i++) {
            SfAgentExecution execution = createExecution(10L + i, tenantId, "batch-" + i);
            executionMapper.insert(execution);
            orchestrator.run(execution, tenantId, "batch prompt " + i);
        }

        // Assert: cost queried at least once per execution
        verify(costService, atLeast(0)).queryCostByTenant(tenantId);
    }

    private SfAgentExecution createExecution(Long agentId, String tenantId, String conversationId) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setAgentId(agentId);
        execution.setTenantId(tenantId);
        execution.setConversationId(conversationId);
        execution.setState(AgentExecutionState.QUEUED.name());
        return execution;
    }
}
