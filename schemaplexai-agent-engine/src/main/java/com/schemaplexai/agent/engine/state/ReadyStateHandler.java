package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReadyStateHandler implements AgentStateHandler {

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.READY;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} ready, execution {}", execution.getAgentId(), execution.getId());
        stateMachine.transition(AgentExecutionState.THINKING, execution);
    }
}
