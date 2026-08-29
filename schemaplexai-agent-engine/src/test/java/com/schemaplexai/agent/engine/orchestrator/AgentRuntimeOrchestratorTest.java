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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
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

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOps;

    @Mock
    private com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService lifecycleService;

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
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
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

    @Test
    void shouldPauseWhenRedisPauseKeyExists() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        // Simulate Redis pause key existing (non-null return).
        // Key is tenant-scoped per TenantRedisKeyResolver: sf:{tenantId}:execution:paused:{executionId}
        when(valueOps.get(contains("sf:" + TENANT_ID + ":execution:paused:"))).thenReturn("USER_REQUEST");

        orchestrator.run(execution, TENANT_ID, PROMPT);

        // Pause check happens before getCurrentState in the loop,
        // so the loop breaks early and transitions to PAUSED
        verify(stateMachine).start(execution);
        verify(stateMachine).transition(AgentExecutionState.PAUSED, execution);
    }

    @Test
    void shouldCancelExecutionViaScopedKeyAndConsumeIt() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        // Fake Redis: cancel() writes the scoped key, isCancelled reads it back.
        java.util.Map<String, String> redis = new java.util.concurrent.ConcurrentHashMap<>();
        doAnswer(inv -> { redis.put(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(valueOps).set(anyString(), anyString(), any(java.time.Duration.class));
        when(valueOps.get(anyString())).thenAnswer(inv -> redis.get(inv.getArgument(0)));

        // Cancel BEFORE the run starts: the signal must survive into the run
        // (the legacy global resetCancelled() could wipe such a pre-run signal).
        orchestrator.cancel(TENANT_ID, 1L);

        // The key must be tenant- and execution-scoped (TenantRedisKeyResolver style).
        String expectedKey = com.schemaplexai.common.redis.TenantRedisKeyResolver.tenantKey(
                TENANT_ID,
                com.schemaplexai.common.redis.TenantRedisKeyResolver.CAT_EXECUTION,
                "cancelled", "1");
        assertEquals("CANCELLED", redis.get(expectedKey));

        orchestrator.run(execution, TENANT_ID, PROMPT);

        // The loop observes the per-execution signal on its first check and cancels
        // before any state is dispatched.
        verify(stateMachine).start(execution);
        verify(stateMachine).transition(AgentExecutionState.CANCELLED, execution);
        verify(stateMachine, never()).getCurrentState(1L);
        verify(stateMachine, never()).transition(AgentExecutionState.COMPLETED, execution);
        // Signal is consumed once observed.
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void cancelOfOneExecutionShouldNotAffectConcurrentExecution() throws Exception {
        when(admissionService.admit(anyString(), anyLong(), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        // Shared fake Redis across both executions — keys are scoped per executionId.
        java.util.Map<String, String> redis = new java.util.concurrent.ConcurrentHashMap<>();
        doAnswer(inv -> { redis.put(inv.getArgument(0), inv.getArgument(1)); return null; })
            .when(valueOps).set(anyString(), anyString(), any(java.time.Duration.class));
        when(valueOps.get(anyString())).thenAnswer(inv -> redis.get(inv.getArgument(0)));
        when(redisTemplate.delete(anyString()))
            .thenAnswer(inv -> redis.remove(inv.getArgument(0)) != null);

        // Each loop iteration takes ~20ms so the cancellation lands mid-flight.
        when(stateMachine.getCurrentState(anyLong())).thenAnswer(inv -> {
            Thread.sleep(20);
            return AgentExecutionState.THINKING;
        });

        SfAgentExecution execA = newExecution(101L, 11L, "conv-A");
        SfAgentExecution execB = newExecution(102L, 12L, "conv-B");

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> runA =
                    pool.submit(() -> orchestrator.run(execA, "tenant-A", "prompt A"));
            java.util.concurrent.Future<?> runB =
                    pool.submit(() -> orchestrator.run(execB, "tenant-B", "prompt B"));

            // Wait until both executions are inside their loops, then cancel ONLY A.
            Thread.sleep(80);
            orchestrator.cancel("tenant-A", 101L);

            runA.get(10, java.util.concurrent.TimeUnit.SECONDS);
            runB.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // The cancelled execution enters the CANCELLED semantic state...
        verify(stateMachine).transition(AgentExecutionState.CANCELLED, execA);
        // ...while the concurrent execution is untouched: it kept running THINKING
        // rounds and was never cancelled.
        verify(stateMachine, never()).transition(AgentExecutionState.CANCELLED, execB);
        verify(stateMachine, atLeastOnce()).transition(AgentExecutionState.THINKING, execB);
    }

    @Test
    void shouldNotForceCompletionOnMaxIterationsWhenPaused() {
        when(admissionService.admit(eq(TENANT_ID), eq(10L), any(TokenBudget.class)))
            .thenReturn(AdmissionResult.builder().allowed(true).build());

        when(stateMachine.getCurrentState(1L))
            .thenReturn(AgentExecutionState.THINKING);

        // Simulate pause key set after some iterations
        // Return null (not paused) for first 25 iterations, then return a value (paused).
        // Key is tenant-scoped per TenantRedisKeyResolver: sf:{tenantId}:execution:paused:{executionId}
        when(valueOps.get(contains("sf:" + TENANT_ID + ":execution:paused:")))
            .thenReturn(null, null, null, null, null, null, null, null, null, null,
                      null, null, null, null, null, null, null, null, null, null,
                      null, null, null, null, null,
                      "USER_REQUEST");

        orchestrator.run(execution, TENANT_ID, PROMPT);

        // Should transition to PAUSED, not COMPLETED
        verify(stateMachine).transition(AgentExecutionState.PAUSED, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.COMPLETED, execution);
    }

    private SfAgentExecution newExecution(Long id, Long agentId, String conversationId) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(agentId);
        e.setConversationId(conversationId);
        e.setCreatedBy(100L);
        e.setState("IDLE");
        return e;
    }
}
