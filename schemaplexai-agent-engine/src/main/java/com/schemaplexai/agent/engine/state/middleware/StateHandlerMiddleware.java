package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;

/**
 * Pluggable middleware that wraps around state handler execution in the agent FSM.
 *
 * <p>Implement this interface to add cross-cutting concerns (logging, metrics,
 * approval gates, audit trails) without modifying individual state handlers.
 *
 * <p>Execution order:
 * <ol>
 *   <li>{@link #before} — called before the state handler executes. If any middleware
 *       returns {@code false}, the handler is skipped and {@link #onSkipped} is called
 *       on all middlewares that already ran {@code before}.</li>
 *   <li>State handler executes (if all {@code before} calls returned {@code true}).</li>
 *   <li>{@link #after} — called after the handler completes (or throws).</li>
 * </ol>
 *
 * <p>Middleware ordering is controlled by {@link #getOrder()} (lower = earlier).
 * Built-in middleware uses ranges:
 * <ul>
 *   <li>0-99: Logging / observability</li>
 *   <li>100-199: Security / approval gates</li>
 *   <li>200-299: Metrics / telemetry</li>
 *   <li>300+: Custom middleware</li>
 * </ul>
 */
public interface StateHandlerMiddleware {

    /**
     * Called before the state handler executes.
     *
     * @param context   the middleware context
     * @return {@code true} to proceed with the handler, {@code false} to skip it
     */
    boolean before(MiddlewareContext context);

    /**
     * Called after the state handler completes (whether successfully or with an error).
     *
     * @param context   the middleware context
     * @param error     the exception thrown by the handler, or {@code null} if successful
     */
    void after(MiddlewareContext context, Throwable error);

    /**
     * Called when the handler was skipped because a preceding middleware returned {@code false}
     * from {@link #before}. Only called on middlewares whose {@code before} already executed.
     *
     * @param context   the middleware context
     */
    default void onSkipped(MiddlewareContext context) {
        // no-op by default
    }

    /**
     * Controls execution order. Lower values execute first.
     */
    int getOrder();
}
