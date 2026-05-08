package com.schemaplexai.agent.engine.state.middleware.impl;

import com.schemaplexai.agent.engine.approval.AuditEntry;
import com.schemaplexai.agent.engine.approval.AuditTrail;
import com.schemaplexai.agent.engine.state.middleware.MiddlewareContext;
import com.schemaplexai.agent.engine.state.middleware.StateHandlerMiddleware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Middleware that records state transitions in the audit trail.
 *
 * <p>Extracts the audit concern from individual state handlers into a
 * centralized, reusable middleware.
 */
@Slf4j
@Component
public class AuditMiddleware implements StateHandlerMiddleware {

    private static final int ORDER = 100;

    private final AuditTrail auditTrail;

    public AuditMiddleware(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public boolean before(MiddlewareContext context) {
        auditTrail.record(new AuditEntry(
                String.valueOf(context.getExecution().getId()),
                String.valueOf(context.getExecution().getAgentId()),
                "STATE_TRANSITION",
                String.format("%s -> %s", context.getPreviousState(), context.getTargetState()),
                Instant.now()
        ));
        return true;
    }

    @Override
    public void after(MiddlewareContext context, Throwable error) {
        if (error != null) {
            auditTrail.record(new AuditEntry(
                    String.valueOf(context.getExecution().getId()),
                    "SYSTEM",
                    "STATE_HANDLER_ERROR",
                    String.format("Handler for %s failed: %s",
                            context.getTargetState(), error.getMessage()),
                    Instant.now()
            ));
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
