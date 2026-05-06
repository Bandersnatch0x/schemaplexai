package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.admission.AdmissionResult;
import com.schemaplexai.agent.engine.admission.ExecutionAdmissionService;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRuntimeOrchestratorIntegrationTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private ExecutionAdmissionService admissionService;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private ObservabilityRecorder observabilityRecorder;

    @InjectMocks
    private AgentRuntimeOrchestrator orchestrator;

    @Test
    void shouldCallObservabilityRecorderDuringExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setConversationId("conv-1");
        execution.setCreatedBy(100L);
        execution.setState("IDLE");

        ObservabilityTrace mockTrace = new ObservabilityTrace();
        mockTrace.setTraceId("trace-123");
        mockTrace.setName("agent-execution-1");
        when(observabilityRecorder.startTrace(
            eq("1"), eq("agent-execution"), eq("100"), eq("conv-1"), anyString()))
            .thenReturn(mockTrace);

        when(admissionService.admit(eq("tenant-1"), eq(1L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L)).thenReturn(AgentExecutionState.COMPLETED);

        orchestrator.run(execution, "tenant-1", "test prompt");

        verify(observabilityRecorder, atLeastOnce())
            .startTrace(any(), any(), any(), any(), any());
        verify(observabilityRecorder, atLeastOnce())
            .endTrace(eq("trace-123"), anyString());
    }
}
