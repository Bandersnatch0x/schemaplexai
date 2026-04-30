package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CompletedStateHandler implements AgentStateHandler {

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.COMPLETED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering COMPLETED state, execution {}", execution.getAgentId(), execution.getId());
        execution.setState(AgentExecutionState.COMPLETED.name());
        execution.setCompletedAt(LocalDateTime.now());
        stateMachine.saveExecution(execution);
    }
}
