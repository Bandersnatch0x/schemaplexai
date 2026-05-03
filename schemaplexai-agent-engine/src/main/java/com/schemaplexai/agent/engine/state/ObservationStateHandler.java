package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.extractor.FinalAnswerExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observation state handler — processes tool execution results and determines the next step.
 * Transitions to COMPLETED if a Final Answer is detected or max iterations are reached,
 * otherwise transitions to REFLECTING for self-evaluation before the next reasoning cycle.
 */
@Slf4j
@Component
public class ObservationStateHandler implements AgentStateHandler {

    private static final int DEFAULT_MAX_ITERATIONS = 10;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.OBSERVATION;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} observing results, execution {}", execution.getAgentId(), execution.getId());

        String lastOutput = execution.getLastOutput();
        int iterationCount = resolveIterationCount(execution);

        // Check if we've reached the max iterations
        if (iterationCount >= DEFAULT_MAX_ITERATIONS) {
            log.info("Max iterations ({}) reached for execution {}, completing", DEFAULT_MAX_ITERATIONS, execution.getId());
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            return;
        }

        // Check if the output contains a Final Answer
        if (lastOutput != null && FinalAnswerExtractor.hasFinalAnswer(lastOutput)) {
            log.info("Final Answer detected in execution {}, transitioning to COMPLETED", execution.getId());
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            return;
        }

        // No Final Answer yet — transition to REFLECTING for self-evaluation before the next cycle
        log.info("No Final Answer yet for execution {}, transitioning to REFLECTING (iteration {})",
                execution.getId(), iterationCount + 1);
        incrementIterationCount(execution);
        stateMachine.transition(AgentExecutionState.REFLECTING, execution);
    }

    /**
     * Extract the current iteration count from execution metadata.
     */
    private int resolveIterationCount(SfAgentExecution execution) {
        String meta = execution.getTokenBudgetJson();
        if (meta != null && meta.contains("iterations=")) {
            try {
                String val = meta.substring(meta.indexOf("iterations=") + 11);
                int commaIdx = val.indexOf(',');
                if (commaIdx > 0) val = val.substring(0, commaIdx);
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Increment the iteration counter stored in execution metadata.
     */
    private void incrementIterationCount(SfAgentExecution execution) {
        int current = resolveIterationCount(execution);
        int next = current + 1;
        String existing = execution.getTokenBudgetJson();
        if (existing == null || existing.isBlank()) {
            execution.setTokenBudgetJson("iterations=" + next);
        } else if (existing.contains("iterations=")) {
            String updated = existing.replaceAll("iterations=\\d+", "iterations=" + next);
            execution.setTokenBudgetJson(updated);
        } else {
            execution.setTokenBudgetJson(existing + ",iterations=" + next);
        }
        stateMachine.saveExecution(execution);
    }
}
