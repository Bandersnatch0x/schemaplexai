package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PausedStateHandler implements AgentStateHandler {

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.PAUSED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering PAUSED state, execution {}", execution.getAgentId(), execution.getId());
        // Persist snapshot and wait for resume signal
        // No automatic transition; external resume triggers transition
    }
}
