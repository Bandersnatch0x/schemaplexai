package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.admission.AdmissionResult;
import com.schemaplexai.agent.engine.admission.ExecutionAdmissionService;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.approval.ApprovalMode;
import com.schemaplexai.agent.engine.approval.ApprovalService;
import com.schemaplexai.agent.engine.approval.ToolApprovalService;
import com.schemaplexai.agent.engine.approval.ToolRiskClassifier;
import com.schemaplexai.agent.engine.config.AgentEngineProperties;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.memory.compaction.AutoCompactionService;
import com.schemaplexai.agent.engine.memory.compaction.CompactionResult;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.ModelResolver;
import com.schemaplexai.agent.engine.mq.AgentExecutionEventPublisher;
import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestrator;
import com.schemaplexai.agent.engine.role.RoleRegistry;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionRecorder;
import com.schemaplexai.agent.engine.tool.ToolSafetyGuard;
import com.schemaplexai.agent.engine.tool.ToolSandbox;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ticket 904 / REQ-09 end-to-end regression: every GATE_BLOCKED entry path must
 * publish the AGENT_GATE_BLOCKED event without throwing and let the state machine
 * proceed to the retry path instead of degrading to FAILED.
 *
 * <p>Entries covered (representative of the five categories):
 * <ol>
 *   <li>Guardrails — ThinkingStateHandler input validation</li>
 *   <li>Loop detection — ThinkingStateHandler loop check</li>
 *   <li>Token budget — ThinkingStateHandler budget gate</li>
 *   <li>Admission — AgentRuntimeOrchestrator admission denial</li>
 *   <li>Tool block — ToolCallingStateHandler safety-guard block</li>
 * </ol>
 *
 * <p>The mocked {@link AgentStateMachine} dispatches GATE_BLOCKED transitions to the
 * REAL {@link GateBlockedStateHandler} backed by the REAL
 * {@link AgentExecutionEventPublisher} (mocked RabbitTemplate), mirroring the
 * production dispatch inside AgentStateMachine#transition.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GateBlockedEntryRegressionTest {

    // --- Shared real components under test ---
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AgentStateMachine stateMachine;

    private AgentExecutionEventPublisher realPublisher;
    private GateBlockedStateHandler gateBlockedHandler;

    // --- ThinkingStateHandler dependencies ---
    @Mock
    private ContextInjector contextInjector;
    @Mock
    private CompositeChatMemoryStore chatMemoryStore;
    @Mock
    private AiModelRouter modelRouter;
    @Mock
    private AgentLoopDetectionService loopDetection;
    @Mock
    private ModelResolver modelResolver;
    @Mock
    private GuardrailsEngine guardrailsEngine;
    @Mock
    private SkillRegistry skillRegistry;
    @Mock
    private RoleRegistry roleRegistry;
    @Mock
    private AutoCompactionService autoCompactionService;
    @Mock
    private com.schemaplexai.agent.engine.tool.ToolRegistry promptToolRegistry;

    private ThinkingStateHandler thinkingHandler;

    // --- ToolCallingStateHandler dependencies ---
    @Mock
    private ToolSandbox sandbox;
    @Mock
    private com.schemaplexai.agent.engine.tool.registry.ToolRegistry toolRegistry;
    @Mock
    private ToolSafetyGuard safetyGuard;
    @Mock
    private ToolExecutionRecorder executionRecorder;
    @Mock
    private SecurityPolicyLoader securityPolicyLoader;
    @Mock
    private AgentEngineProperties engineProperties;
    @Mock
    private ApprovalService approvalService;
    @Mock
    private ToolAdapter toolAdapter;

    private ToolCallingStateHandler toolCallingHandler;

    // --- AgentRuntimeOrchestrator dependencies ---
    @Mock
    private ExecutionAdmissionService admissionService;
    @Mock
    private ObservabilityRecorder observabilityRecorder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AgentExecutionLifecycleService lifecycleService;

    private AgentRuntimeOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        realPublisher = new AgentExecutionEventPublisher(rabbitTemplate, new ObjectMapper());
        gateBlockedHandler = new GateBlockedStateHandler(realPublisher);

        thinkingHandler = new ThinkingStateHandler(
                contextInjector, chatMemoryStore, modelRouter, loopDetection, modelResolver,
                guardrailsEngine, skillRegistry, roleRegistry, autoCompactionService,
                promptToolRegistry, null);

        ToolApprovalService toolApprovalService = new ToolApprovalService(
                approvalService, new ToolRiskClassifier(), ApprovalMode.AUTO, 60);
        toolCallingHandler = new ToolCallingStateHandler(
                chatMemoryStore, sandbox, toolRegistry, safetyGuard, loopDetection,
                executionRecorder, securityPolicyLoader, engineProperties, toolApprovalService,
                modelRouter, modelResolver, null);

        orchestrator = new AgentRuntimeOrchestrator(
                stateMachine, admissionService, chatMemoryStore, observabilityRecorder,
                engineProperties, redisTemplate, lifecycleService);

        // Shared lenient defaults for the THINKING entry paths
        when(guardrailsEngine.validateInput(anyString())).thenReturn(ValidationResult.valid());
        when(autoCompactionService.compactIfNeeded(anyString(), any()))
                .thenReturn(CompactionResult.empty());
        when(skillRegistry.resolveAvailable(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Collections.emptyList());
        when(promptToolRegistry.getAll()).thenReturn(Collections.emptyList());
    }

    /**
     * Mirrors AgentStateMachine#transition: a GATE_BLOCKED transition dispatches the
     * real handler; all other transitions are recorded for verification.
     */
    private void wireGateBlockedDispatch(SfAgentExecution execution) {
        doAnswer(invocation -> {
            AgentExecutionState target = invocation.getArgument(0);
            SfAgentExecution exec = invocation.getArgument(1);
            if (target == AgentExecutionState.GATE_BLOCKED) {
                gateBlockedHandler.handle(stateMachine, exec);
            }
            return null;
        }).when(stateMachine).transition(any(AgentExecutionState.class), eq(execution));
    }

    @Test
    void guardrailsEntryShouldPublishGateBlockedAndProceedToRetrying() throws Exception {
        SfAgentExecution execution = newExecution(101L, "conv-guardrails");
        wireGateBlockedDispatch(execution);
        when(chatMemoryStore.loadMessages("conv-guardrails"))
                .thenReturn(List.of(new LlmMessage("user", "hello")));
        when(guardrailsEngine.validateInput(anyString()))
                .thenReturn(ValidationResult.invalid("blocked keyword: jailbreak"));

        assertDoesNotThrow(() -> thinkingHandler.handle(stateMachine, execution));

        assertRetryableGateBlockedPublished(execution, "blocked keyword: jailbreak");
    }

    @Test
    void loopDetectionEntryShouldPublishGateBlockedAndProceedToRetrying() throws Exception {
        SfAgentExecution execution = newExecution(102L, "conv-loop");
        wireGateBlockedDispatch(execution);
        when(chatMemoryStore.loadMessages("conv-loop")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Let me call a tool <tool_use><name>search</name></tool_use>");
        when(loopDetection.detectLoop(anyLong(), anyString(), anyList()))
                .thenReturn(LoopDetectionResult.hashLoop());

        assertDoesNotThrow(() -> thinkingHandler.handle(stateMachine, execution));

        assertRetryableGateBlockedPublished(execution, "agent_loop_HASH_LOOP");
    }

    @Test
    void tokenBudgetEntryShouldPublishGateBlockedAndProceedToRetrying() throws Exception {
        SfAgentExecution execution = newExecution(103L, "conv-budget");
        execution.setTokenBudgetJson("10,10,0,0");
        wireGateBlockedDispatch(execution);
        when(chatMemoryStore.loadMessages("conv-budget")).thenReturn(List.of(
                new LlmMessage("user", "This is a very long message that will exceed the tiny token budget")));

        assertDoesNotThrow(() -> thinkingHandler.handle(stateMachine, execution));

        assertRetryableGateBlockedPublished(execution, "token_budget_exceeded");
    }

    @Test
    void admissionDeniedEntryShouldPublishGateBlockedAndProceedToRetrying() throws Exception {
        SfAgentExecution execution = newExecution(104L, "conv-admission");
        execution.setCreatedBy(100L);
        wireGateBlockedDispatch(execution);

        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId("trace-104");
        when(observabilityRecorder.startTrace(any(), any(), any(), any(), any())).thenReturn(trace);
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(admissionService.admit(eq("tenant-1"), eq(42L), any(TokenBudget.class)))
                .thenReturn(AdmissionResult.builder().allowed(false).reason("quota exceeded").build());

        assertDoesNotThrow(() -> orchestrator.run(execution, "tenant-1", "test prompt"));

        // Admission denial carries no blockedReason metadata — the handler records
        // the documented default and still proceeds to the retry path.
        assertRetryableGateBlockedPublished(execution, "admission_denied");
        verify(stateMachine, never()).start(any());
    }

    @Test
    void toolBlockedEntryShouldPublishGateBlockedAndProceedToRetrying() throws Exception {
        SfAgentExecution execution = newExecution(105L, "conv-toolblock");
        wireGateBlockedDispatch(execution);
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(10);
        when(chatMemoryStore.loadMessages("conv-toolblock"))
                .thenReturn(List.of(new LlmMessage("assistant", "calling volumeDelete")));
        when(toolRegistry.parse("calling volumeDelete", null))
                .thenReturn(List.of(new ToolCall("volumeDelete")));
        when(loopDetection.detectLoop(eq(105L), anyString(), anyList()))
                .thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("volumeDelete")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-1")).thenReturn(null);
        when(safetyGuard.check("volumeDelete", "{}", "tenant-1")).thenReturn(
                new ToolSafetyGuard.SafetyCheckResult(
                        false, true, ToolErrorCategory.IRREVERSIBLE_OPERATION, "Irreversible"));

        assertDoesNotThrow(() -> toolCallingHandler.handle(stateMachine, execution));

        // Tool-block path sets no blockedReason metadata — handler uses the default.
        assertRetryableGateBlockedPublished(execution, "admission_denied");
    }

    /**
     * Asserts the AGENT_GATE_BLOCKED event reached MQ with the spec-required fields,
     * and the state machine entered the retry path instead of FAILED.
     */
    private void assertRetryableGateBlockedPublished(SfAgentExecution execution, String expectedReason)
            throws Exception {
        // Event published successfully (no UnsupportedOperationException escaped)
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_AGENT_EXEC_EVENT),
                messageCaptor.capture());

        Map<String, Object> sent = new ObjectMapper().readValue(messageCaptor.getValue(), Map.class);
        assertEquals("AGENT_GATE_BLOCKED", sent.get("eventType"));
        assertNotNull(sent.get("timestamp"));
        assertEquals(execution.getId(), ((Number) sent.get("executionId")).longValue());
        assertEquals(execution.getAgentId(), ((Number) sent.get("agentId")).longValue());
        assertEquals(expectedReason, sent.get("reason"));
        assertEquals(true, sent.get("retryable"));
        assertEquals(60, ((Number) sent.get("retryCountdown")).intValue());

        // State machine proceeds to the retry path, not directly to FAILED
        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.FAILED, execution);
    }

    private SfAgentExecution newExecution(long id, String conversationId) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(id);
        execution.setAgentId(42L);
        execution.setConversationId(conversationId);
        execution.setTenantId("tenant-1");
        execution.setTokenBudgetJson("32000,4096,0,0");
        return execution;
    }
}
