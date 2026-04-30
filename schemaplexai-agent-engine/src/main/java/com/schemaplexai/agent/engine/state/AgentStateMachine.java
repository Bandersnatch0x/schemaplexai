package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AgentStateMachine {

    private final SfAgentExecutionMapper executionMapper;
    private final Map<AgentExecutionState, AgentStateHandler> handlers;
    private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();

    @Autowired
    public AgentStateMachine(SfAgentExecutionMapper executionMapper, List<AgentStateHandler> handlerList) {
        this.executionMapper = executionMapper;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(AgentStateHandler::getState, Function.identity()));
    }

    public void start(SfAgentExecution execution) {
        transition(AgentExecutionState.INITIALIZING, execution);
    }

    public void transition(AgentExecutionState newState, SfAgentExecution execution) {
        AgentExecutionState current = executionStates.get(execution.getId());
        if (current != null && current.isTerminal()) {
            log.warn("Cannot transition from terminal state {} to {} for execution {}",
                     current, newState, execution.getId());
            return;
        }
        log.info("Execution {} transitioning from {} to {}", execution.getId(), current, newState);
        execution.setState(newState.name());
        saveExecution(execution);
        executionStates.put(execution.getId(), newState);

        if (newState.isTerminal()) {
            removeExecution(execution.getId());
        }

        AgentStateHandler handler = handlers.get(newState);
        if (handler != null) {
            try {
                handler.handle(this, execution);
            } catch (Exception e) {
                log.error("State handler error for state {} execution {}", newState, execution.getId(), e);
                transition(AgentExecutionState.FAILED, execution);
            }
        }
    }

    public void saveExecution(SfAgentExecution execution) {
        executionMapper.updateById(execution);
    }

    public void removeExecution(Long executionId) {
        executionStates.remove(executionId);
    }

    public AgentExecutionState getCurrentState(Long executionId) {
        return executionStates.get(executionId);
    }
}
