package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.approval.AuditEntry;
import com.schemaplexai.agent.engine.approval.AuditTrail;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.state.middleware.impl.AuditMiddleware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditMiddlewareTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private AuditTrail auditTrail;

    @Test
    void beforeShouldRecordStateTransition() {
        AuditMiddleware middleware = new AuditMiddleware(auditTrail);
        SfAgentExecution execution = createExecution(1L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution,
                AgentExecutionState.READY, AgentExecutionState.THINKING);

        assertTrue(middleware.before(context));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditTrail).record(captor.capture());
        AuditEntry entry = captor.getValue();
        assertEquals("STATE_TRANSITION", entry.action());
        assertTrue(entry.detail().contains("READY"));
        assertTrue(entry.detail().contains("THINKING"));
    }

    @Test
    void afterShouldNotRecordOnSuccess() {
        AuditMiddleware middleware = new AuditMiddleware(auditTrail);
        SfAgentExecution execution = createExecution(2L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.COMPLETED);

        middleware.after(context, null);

        verify(auditTrail, never()).record(any());
    }

    @Test
    void afterShouldRecordError() {
        AuditMiddleware middleware = new AuditMiddleware(auditTrail);
        SfAgentExecution execution = createExecution(3L);
        MiddlewareContext context = new MiddlewareContext(
                stateMachine, execution, null, AgentExecutionState.FAILED);

        middleware.after(context, new RuntimeException("boom"));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditTrail).record(captor.capture());
        AuditEntry entry = captor.getValue();
        assertEquals("STATE_HANDLER_ERROR", entry.action());
        assertTrue(entry.detail().contains("boom"));
    }

    @Test
    void orderShouldBe100() {
        AuditMiddleware middleware = new AuditMiddleware(auditTrail);
        assertEquals(100, middleware.getOrder());
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        return e;
    }
}
