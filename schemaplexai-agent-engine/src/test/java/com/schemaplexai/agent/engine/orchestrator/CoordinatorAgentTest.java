package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoordinatorAgentTest {

    @Mock
    private AgentExecutionEngine executionEngine;

    @Spy
    private AgentRouter agentRouter = new AgentRouter();

    @Mock
    private SequentialAgentExecutor sequentialExecutor;

    @Mock
    private ParallelAgentExecutor parallelExecutor;

    @InjectMocks
    private CoordinatorAgent coordinatorAgent;

    private List<AgentRouter.AgentCapability> availableAgents;
    private AgentRouter.AgentCapability coderAgent;
    private AgentRouter.AgentCapability reviewerAgent;

    @BeforeEach
    void setUp() {
        coderAgent = new AgentRouter.AgentCapability(
                "coder-1", "Writes code", Set.of("code", "implement", "function"), 3
        );
        reviewerAgent = new AgentRouter.AgentCapability(
                "reviewer-1", "Reviews code", Set.of("review", "check"), 2
        );
        availableAgents = List.of(coderAgent, reviewerAgent);
    }

    @Test
    void shouldDelegateToSequentialExecutorForSequentialStrategy() {
        CoordinatorAgent.CoordinationRequest request = new CoordinatorAgent.CoordinationRequest(
                1L, "tenant-1",
                List.of("Write function A", "Write function B"),
                CoordinatorAgent.CoordinationStrategy.SEQUENTIAL
        );

        List<SubTaskResult> expectedResults = List.of(
                new SubTaskResult("Write function A", "coder-1", SubTaskStatus.COMPLETED, null),
                new SubTaskResult("Write function B", "coder-1", SubTaskStatus.COMPLETED, null)
        );
        when(sequentialExecutor.execute(request, availableAgents)).thenReturn(expectedResults);

        CoordinatorAgent.CoordinationResult result = coordinatorAgent.coordinate(request, availableAgents);

        assertThat(result.allSucceeded()).isTrue();
        assertThat(result.subTaskResults()).hasSize(2);
        verify(sequentialExecutor).execute(request, availableAgents);
        verifyNoInteractions(parallelExecutor);
    }

    @Test
    void shouldDelegateToParallelExecutorForParallelStrategy() {
        CoordinatorAgent.CoordinationRequest request = new CoordinatorAgent.CoordinationRequest(
                1L, "tenant-1",
                List.of("Write function A", "Write function B"),
                CoordinatorAgent.CoordinationStrategy.PARALLEL
        );

        List<SubTaskResult> expectedResults = List.of(
                new SubTaskResult("Write function A", "coder-1", SubTaskStatus.COMPLETED, null),
                new SubTaskResult("Write function B", "coder-1", SubTaskStatus.COMPLETED, null)
        );
        when(parallelExecutor.execute(request, availableAgents)).thenReturn(expectedResults);

        CoordinatorAgent.CoordinationResult result = coordinatorAgent.coordinate(request, availableAgents);

        assertThat(result.allSucceeded()).isTrue();
        verify(parallelExecutor).execute(request, availableAgents);
        verifyNoInteractions(sequentialExecutor);
    }

    @Test
    void shouldReportPartialFailure() {
        CoordinatorAgent.CoordinationRequest request = new CoordinatorAgent.CoordinationRequest(
                1L, "tenant-1",
                List.of("Write function A", "Impossible task"),
                CoordinatorAgent.CoordinationStrategy.SEQUENTIAL
        );

        List<SubTaskResult> expectedResults = List.of(
                new SubTaskResult("Write function A", "coder-1", SubTaskStatus.COMPLETED, null),
                new SubTaskResult("Impossible task", "reviewer-1", SubTaskStatus.FAILED, "No match")
        );
        when(sequentialExecutor.execute(request, availableAgents)).thenReturn(expectedResults);

        CoordinatorAgent.CoordinationResult result = coordinatorAgent.coordinate(request, availableAgents);

        assertThat(result.allSucceeded()).isFalse();
        assertThat(result.subTaskResults()).hasSize(2);
        assertThat(result.subTaskResults().get(0).isSuccess()).isTrue();
        assertThat(result.subTaskResults().get(1).isSuccess()).isFalse();
    }

    @Test
    void shouldReportAllSucceededWhenAllComplete() {
        CoordinatorAgent.CoordinationRequest request = new CoordinatorAgent.CoordinationRequest(
                1L, "tenant-1",
                List.of("Task A", "Task B", "Task C"),
                CoordinatorAgent.CoordinationStrategy.PARALLEL
        );

        List<SubTaskResult> expectedResults = List.of(
                new SubTaskResult("Task A", "coder-1", SubTaskStatus.COMPLETED, null),
                new SubTaskResult("Task B", "reviewer-1", SubTaskStatus.COMPLETED, null),
                new SubTaskResult("Task C", "coder-1", SubTaskStatus.COMPLETED, null)
        );
        when(parallelExecutor.execute(request, availableAgents)).thenReturn(expectedResults);

        CoordinatorAgent.CoordinationResult result = coordinatorAgent.coordinate(request, availableAgents);

        assertThat(result.allSucceeded()).isTrue();
    }

    @Test
    void shouldDispatchSubAgentViaExecutionEngine() {
        SfAgentExecution mockExecution = new SfAgentExecution();
        mockExecution.setId(42L);
        when(executionEngine.startExecution(eq(10L), eq("tenant-1"), eq("test prompt")))
                .thenReturn(mockExecution);

        SfAgentExecution result = coordinatorAgent.dispatchSubAgent(10L, "tenant-1", "test prompt");

        assertThat(result.getId()).isEqualTo(42L);
        verify(executionEngine).startExecution(10L, "tenant-1", "test prompt");
    }

    @Test
    void coordinationRequestShouldRejectNullParentExecutionId() {
        assertThatThrownBy(() ->
                new CoordinatorAgent.CoordinationRequest(null, "tenant-1", List.of("task"), null)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentExecutionId");
    }

    @Test
    void coordinationRequestShouldRejectBlankTenantId() {
        assertThatThrownBy(() ->
                new CoordinatorAgent.CoordinationRequest(1L, "", List.of("task"), null)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void coordinationRequestShouldRejectEmptySubTasks() {
        assertThatThrownBy(() ->
                new CoordinatorAgent.CoordinationRequest(1L, "tenant-1", List.of(), null)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subTasks");
    }

    @Test
    void coordinationRequestShouldDefaultStrategyToSequential() {
        CoordinatorAgent.CoordinationRequest request = new CoordinatorAgent.CoordinationRequest(
                1L, "tenant-1", List.of("task"), null
        );

        assertThat(request.strategy()).isEqualTo(CoordinatorAgent.CoordinationStrategy.SEQUENTIAL);
    }
}
