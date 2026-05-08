package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.admission.AdmissionResult;
import com.schemaplexai.agent.engine.admission.ExecutionAdmissionService;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRuntimeOrchestratorTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private ExecutionAdmissionService admissionService;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private ObservabilityRecorder observabilityRecorder;

    @Mock
    private com.schemaplexai.agent.engine.config.AgentEngineProperties engineProperties;

    @InjectMocks
    private AgentRuntimeOrchestrator orchestrator;

    private SfAgentExecution execution;
    private static final String TENANT_ID = "tenant-1";
    private static final String PROMPT = "test prompt";
    private static final String TRACE_ID = "trace-abc";

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(10L);
        execution.setConversationId("conv-1");
        execution.setCreatedBy(100L);
        execution.setState("IDLE");

        ObservabilityTrace mockTrace = new ObservabilityTrace();
        mockTrace.setTraceId(TRACE_ID);
        when(observabilityRecorder.startTrace(any(), any(), any(), any(), any()))
            .thenReturn(mockTrace);

        when(engineProperties.getMaxToolCalls()).thenReturn(10);
    }

    @Test
    void shouldDenyAdmissionAndTransitionToGateBlocked() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(false).reason("quota exceeded").build());

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        verify(stateMachine, never()).start(any());
        verify(observabilityRecorder).endTrace(eq(TRACE_ID), anyString());
        verify(admissionService).releaseConcurrency(TENANT_ID, 10L);
    }

    @Test
    void shouldSaveUserPromptAndStartStateMachineWhenAdmitted() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L)).thenReturn(AgentExecutionState.COMPLETED);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        ArgumentCaptor<LlmMessage> messageCaptor = ArgumentCaptor.forClass(LlmMessage.class);
        verify(chatMemoryStore).saveMessage(eq("conv-1"), messageCaptor.capture());
        LlmMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getRole()).isEqualTo("user");
        assertThat(savedMessage.getContent()).isEqualTo(PROMPT);

        verify(stateMachine).start(execution);
        verify(stateMachine, atLeastOnce()).getCurrentState(1L);
    }

    @Test
    void shouldRunMultipleIterationsUntilTerminalState() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L))
            .thenReturn(AgentExecutionState.THINKING)
            .thenReturn(AgentExecutionState.TOOL_CALLING)
            .thenReturn(AgentExecutionState.COMPLETED);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine, times(3)).getCurrentState(1L);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
        verify(stateMachine).transition(AgentExecutionState.TOOL_CALLING, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void shouldForceCompletionWhenMaxIterationsReached() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L))
            .thenReturn(AgentExecutionState.THINKING);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine, times(50)).getCurrentState(1L);
        verify(stateMachine, times(50)).transition(AgentExecutionState.THINKING, execution);
        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void shouldBreakLoopWhenCurrentStateIsNull() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L)).thenReturn(null);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine, times(1)).getCurrentState(1L);
        verify(stateMachine, never()).transition(any(), eq(execution));
    }

    @Test
    void shouldTransitionToFailedOnException() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        doThrow(new RuntimeException("state machine error")).when(stateMachine).start(any());

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(observabilityRecorder).endTrace(eq(TRACE_ID), anyString());
        verify(admissionService).releaseConcurrency(TENANT_ID, 10L);
    }

    @Test
    void shouldHandleExceptionDuringFailedTransition() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        doThrow(new RuntimeException("start error")).when(stateMachine).start(any());
        doThrow(new RuntimeException("transition error"))
            .when(stateMachine).transition(AgentExecutionState.FAILED, execution);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(observabilityRecorder).endTrace(eq(TRACE_ID), anyString());
        verify(admissionService).releaseConcurrency(TENANT_ID, 10L);
    }

    @Test
    void shouldSetTokenBudgetJsonOnExecution() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L)).thenReturn(AgentExecutionState.COMPLETED);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        assertThat(execution.getTokenBudgetJson()).isNotNull();
        assertThat(execution.getTokenBudgetJson()).contains(",");
    }

    @Test
    void shouldAlwaysReleaseConcurrencyAndEndTrace() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L)).thenReturn(AgentExecutionState.COMPLETED);

        orchestrator.run(execution, TENANT_ID, PROMPT);

        verify(observabilityRecorder).endTrace(eq(TRACE_ID), contains("IDLE"));
        verify(admissionService).releaseConcurrency(TENANT_ID, 10L);
    }
}
