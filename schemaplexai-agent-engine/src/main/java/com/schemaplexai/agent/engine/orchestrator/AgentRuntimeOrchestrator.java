package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.admission.ExecutionAdmissionService;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeOrchestrator {

    private final AgentStateMachine stateMachine;
    private final ExecutionAdmissionService admissionService;
    private final CompositeChatMemoryStore chatMemoryStore;

    private static final int MAX_ITERATIONS = 50;

    public void run(SfAgentExecution execution, String tenantId, String prompt) {
        try {
            // Initialize token budget
            TokenBudget tokenBudget = new TokenBudget(
                    CommonConstants.DEFAULT_MAX_INPUT_TOKENS,
                    CommonConstants.DEFAULT_MAX_OUTPUT_TOKENS
            );
            execution.setTokenBudgetJson(serializeBudget(tokenBudget));

            // Admission check
            var admission = admissionService.admit(tenantId, execution.getAgentId(), tokenBudget);
            if (!admission.isAllowed()) {
                log.warn("Execution {} admission denied: {}", execution.getId(), admission.getReason());
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // Save user prompt to memory
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("user", prompt));

            // Start the state machine
            stateMachine.start(execution);

            // State machine loop with iteration guard
            int iteration = 0;
            while (iteration < MAX_ITERATIONS) {
                AgentExecutionState currentState = stateMachine.getCurrentState(execution.getId());
                if (currentState == null || currentState.isTerminal()) {
                    break;
                }
                stateMachine.transition(currentState, execution);
                iteration++;
            }

            if (iteration >= MAX_ITERATIONS) {
                log.warn("Execution {} hit max iterations, forcing completion", execution.getId());
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            }
        } catch (Exception e) {
            log.error("Execution {} failed", execution.getId(), e);
            try {
                stateMachine.transition(AgentExecutionState.FAILED, execution);
            } catch (Exception ex) {
                log.error("Failed to transition execution {} to FAILED", execution.getId(), ex);
            }
        } finally {
            admissionService.releaseConcurrency(tenantId, execution.getAgentId());
        }
    }

    private String serializeBudget(TokenBudget budget) {
        return budget.getMaxInputTokens() + "," + budget.getMaxOutputTokens() + ",0,0";
    }
}
