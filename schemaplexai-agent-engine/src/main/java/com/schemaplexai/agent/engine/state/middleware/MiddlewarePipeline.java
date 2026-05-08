package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes the middleware pipeline around state handler invocations.
 *
 * <p>Collects all registered {@link StateHandlerMiddleware} beans, sorts them by
 * {@link StateHandlerMiddleware#getOrder()}, and executes them in order.
 *
 * <p>If any middleware's {@code before()} returns {@code false}, the handler is skipped
 * and {@code onSkipped()} is called on all middlewares that already ran.
 */
@Slf4j
@Component
public class MiddlewarePipeline {

    private final List<StateHandlerMiddleware> middlewares;

    public MiddlewarePipeline(List<StateHandlerMiddleware> middlewares) {
        this.middlewares = middlewares.stream()
                .sorted(Comparator.comparingInt(StateHandlerMiddleware::getOrder))
                .toList();
    }

    /**
     * Wraps handler execution with the middleware chain.
     *
     * @param stateMachine the state machine
     * @param execution    the current execution
     * @param previousState the state before transition (may be null for initial transition)
     * @param targetState  the state being entered
     * @param handler      the actual handler to execute if all middlewares approve
     */
    public void execute(AgentStateMachine stateMachine,
                        SfAgentExecution execution,
                        AgentExecutionState previousState,
                        AgentExecutionState targetState,
                        Runnable handler) {
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, previousState, targetState);

        List<StateHandlerMiddleware> executedBefore = new ArrayList<>();

        try {
            // Phase 1: before()
            for (StateHandlerMiddleware middleware : middlewares) {
                if (!middleware.before(context)) {
                    log.info("Middleware {} blocked transition {} -> {} for execution {}",
                            middleware.getClass().getSimpleName(),
                            previousState, targetState, execution.getId());
                    // Notify all middlewares that already ran before() that the handler was skipped
                    for (StateHandlerMiddleware skipped : executedBefore) {
                        skipped.onSkipped(context);
                    }
                    return;
                }
                executedBefore.add(middleware);
            }

            // Phase 2: execute handler
            handler.run();

            // Phase 3: after() — success
            for (StateHandlerMiddleware middleware : middlewares) {
                middleware.after(context, null);
            }
        } catch (Exception e) {
            // Phase 3: after() — error
            for (StateHandlerMiddleware middleware : middlewares) {
                try {
                    middleware.after(context, e);
                } catch (Exception middlewareError) {
                    log.error("Middleware {} after() failed for execution {}",
                            middleware.getClass().getSimpleName(), execution.getId(), middlewareError);
                }
            }
            throw e;
        }
    }

    /**
     * Returns the number of registered middlewares.
     */
    public int size() {
        return middlewares.size();
    }

    /**
     * Returns an unmodifiable view of the registered middlewares (sorted by order).
     */
    public List<StateHandlerMiddleware> getMiddlewares() {
        return List.copyOf(middlewares);
    }
}
