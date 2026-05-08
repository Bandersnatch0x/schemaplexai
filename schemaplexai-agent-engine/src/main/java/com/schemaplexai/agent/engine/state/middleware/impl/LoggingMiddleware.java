package com.schemaplexai.agent.engine.state.middleware.impl;

import com.schemaplexai.agent.engine.state.middleware.MiddlewareContext;
import com.schemaplexai.agent.engine.state.middleware.StateHandlerMiddleware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Middleware that logs state transitions with structured context.
 *
 * <p>Extracts the logging concern that was previously duplicated across
 * all 15 state handlers.
 */
@Slf4j
@Component
public class LoggingMiddleware implements StateHandlerMiddleware {

    private static final int ORDER = 0;

    @Override
    public boolean before(MiddlewareContext context) {
        log.info("State transition: execution={} agent={} {} -> {}",
                context.getExecution().getId(),
                context.getExecution().getAgentId(),
                context.getPreviousState(),
                context.getTargetState());
        return true;
    }

    @Override
    public void after(MiddlewareContext context, Throwable error) {
        if (error != null) {
            log.error("State handler failed: execution={} state={} error={}",
                    context.getExecution().getId(),
                    context.getTargetState(),
                    error.getMessage(), error);
        } else {
            long durationMs = java.time.Duration.between(
                    context.getStartedAt(), java.time.Instant.now()).toMillis();
            log.debug("State handler completed: execution={} state={} durationMs={}",
                    context.getExecution().getId(),
                    context.getTargetState(),
                    durationMs);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
