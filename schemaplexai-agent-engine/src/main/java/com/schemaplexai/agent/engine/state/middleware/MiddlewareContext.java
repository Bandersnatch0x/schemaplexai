package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable context passed through the middleware pipeline during a state transition.
 *
 * <p>Carries the execution, target state, and a shared attributes map for
 * inter-middleware communication (e.g. a logging middleware can store a
 * correlation ID that a metrics middleware reads later).
 */
public final class MiddlewareContext {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecution execution;
    private final AgentExecutionState previousState;
    private final AgentExecutionState targetState;
    private final Instant startedAt;
    private final Map<String, Object> attributes;

    public MiddlewareContext(AgentStateMachine stateMachine,
                            SfAgentExecution execution,
                            AgentExecutionState previousState,
                            AgentExecutionState targetState) {
        this.stateMachine = stateMachine;
        this.execution = execution;
        this.previousState = previousState;
        this.targetState = targetState;
        this.startedAt = Instant.now();
        this.attributes = new ConcurrentHashMap<>();
    }

    public AgentStateMachine getStateMachine() {
        return stateMachine;
    }

    public SfAgentExecution getExecution() {
        return execution;
    }

    public AgentExecutionState getPreviousState() {
        return previousState;
    }

    public AgentExecutionState getTargetState() {
        return targetState;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Shared attribute map for inter-middleware communication.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
