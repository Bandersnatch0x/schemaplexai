package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class GateBlockedStateHandler implements AgentStateHandler {

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.GATE_BLOCKED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.warn("Agent {} gate blocked, execution {}", execution.getAgentId(), execution.getId());
        execution.setState(AgentExecutionState.GATE_BLOCKED.name());
        execution.setCompletedAt(LocalDateTime.now());
        stateMachine.saveExecution(execution);
    }
}
