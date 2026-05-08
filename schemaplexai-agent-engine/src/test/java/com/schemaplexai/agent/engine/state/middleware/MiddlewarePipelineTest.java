package com.schemaplexai.agent.engine.state.middleware;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiddlewarePipelineTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Test
    void shouldExecuteHandlerWhenAllMiddlewaresApprove() {
        // Arrange
        SfAgentExecution execution = createExecution(1L);
        AtomicBoolean m1BeforeCalled = new AtomicBoolean(false);
        AtomicBoolean m2BeforeCalled = new AtomicBoolean(false);
        AtomicBoolean m1AfterCalled = new AtomicBoolean(false);
        AtomicBoolean m2AfterCalled = new AtomicBoolean(false);

        StateHandlerMiddleware m1 = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { m1BeforeCalled.set(true); return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) { m1AfterCalled.set(true); }
            @Override public int getOrder() { return 10; }
        };
        StateHandlerMiddleware m2 = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { m2BeforeCalled.set(true); return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) { m2AfterCalled.set(true); }
            @Override public int getOrder() { return 20; }
        };
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(m1, m2));

        Runnable handler = mock(Runnable.class);

        // Act
        pipeline.execute(stateMachine, execution, null,
                AgentExecutionState.INITIALIZING, handler);

        // Assert
        verify(handler).run();
        assertTrue(m1BeforeCalled.get());
        assertTrue(m2BeforeCalled.get());
        assertTrue(m1AfterCalled.get());
        assertTrue(m2AfterCalled.get());
    }

    @Test
    void shouldSkipHandlerWhenMiddlewareBlocks() {
        // Arrange
        SfAgentExecution execution = createExecution(2L);
        AtomicBoolean blockerBeforeCalled = new AtomicBoolean(false);
        AtomicBoolean laterBeforeCalled = new AtomicBoolean(false);

        StateHandlerMiddleware blocker = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { blockerBeforeCalled.set(true); return false; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public int getOrder() { return 10; }
        };
        StateHandlerMiddleware later = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { laterBeforeCalled.set(true); return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public int getOrder() { return 20; }
        };
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(blocker, later));

        Runnable handler = mock(Runnable.class);

        // Act
        pipeline.execute(stateMachine, execution, null,
                AgentExecutionState.INITIALIZING, handler);

        // Assert
        verify(handler, never()).run();
        assertTrue(blockerBeforeCalled.get());
        assertFalse(laterBeforeCalled.get());
    }

    @Test
    void shouldCallOnSkippedForMiddlewaresThatAlreadyRan() {
        // Arrange
        SfAgentExecution execution = createExecution(3L);
        AtomicBoolean firstOnSkippedCalled = new AtomicBoolean(false);
        AtomicBoolean blockerOnSkippedCalled = new AtomicBoolean(false);

        StateHandlerMiddleware first = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public void onSkipped(MiddlewareContext ctx) { firstOnSkippedCalled.set(true); }
            @Override public int getOrder() { return 10; }
        };
        StateHandlerMiddleware blocker = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { return false; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public void onSkipped(MiddlewareContext ctx) { blockerOnSkippedCalled.set(true); }
            @Override public int getOrder() { return 20; }
        };
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(first, blocker));

        Runnable handler = mock(Runnable.class);

        // Act
        pipeline.execute(stateMachine, execution, null,
                AgentExecutionState.INITIALIZING, handler);

        // Assert
        assertTrue(firstOnSkippedCalled.get());
        assertFalse(blockerOnSkippedCalled.get());
    }

    @Test
    void shouldCallAfterWithErrorWhenHandlerThrows() {
        // Arrange
        SfAgentExecution execution = createExecution(4L);
        AtomicReference<Throwable> capturedError = new AtomicReference<>();

        StateHandlerMiddleware middleware = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) { capturedError.set(err); }
            @Override public int getOrder() { return 10; }
        };
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(middleware));

        Runnable handler = mock(Runnable.class);
        RuntimeException error = new RuntimeException("handler failed");
        doThrow(error).when(handler).run();

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                pipeline.execute(stateMachine, execution, null,
                        AgentExecutionState.THINKING, handler));

        assertSame(error, capturedError.get());
    }

    @Test
    void shouldSortMiddlewaresByOrder() {
        // Arrange
        StateHandlerMiddleware low = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public int getOrder() { return 10; }
        };
        StateHandlerMiddleware high = new StateHandlerMiddleware() {
            @Override public boolean before(MiddlewareContext ctx) { return true; }
            @Override public void after(MiddlewareContext ctx, Throwable err) {}
            @Override public int getOrder() { return 100; }
        };
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(high, low));

        // Assert - low order should be first
        assertEquals(2, pipeline.size());
        assertSame(low, pipeline.getMiddlewares().get(0));
        assertSame(high, pipeline.getMiddlewares().get(1));
    }

    @Test
    void shouldHandleEmptyMiddlewareList() {
        // Arrange
        SfAgentExecution execution = createExecution(5L);
        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of());
        Runnable handler = mock(Runnable.class);

        // Act
        pipeline.execute(stateMachine, execution, null,
                AgentExecutionState.INITIALIZING, handler);

        // Assert
        verify(handler).run();
    }

    @Test
    void shouldPassContextWithCorrectStateInfo() {
        // Arrange
        SfAgentExecution execution = createExecution(6L);
        List<MiddlewareContext> capturedContexts = new ArrayList<>();

        StateHandlerMiddleware capturing = new StateHandlerMiddleware() {
            @Override
            public boolean before(MiddlewareContext context) {
                capturedContexts.add(context);
                return true;
            }
            @Override
            public void after(MiddlewareContext context, Throwable error) {}
            @Override
            public int getOrder() { return 10; }
        };

        MiddlewarePipeline pipeline = new MiddlewarePipeline(List.of(capturing));

        // Act
        pipeline.execute(stateMachine, execution,
                AgentExecutionState.READY, AgentExecutionState.THINKING,
                mock(Runnable.class));

        // Assert
        assertEquals(1, capturedContexts.size());
        MiddlewareContext ctx = capturedContexts.get(0);
        assertEquals(AgentExecutionState.READY, ctx.getPreviousState());
        assertEquals(AgentExecutionState.THINKING, ctx.getTargetState());
        assertSame(execution, ctx.getExecution());
        assertNotNull(ctx.getStartedAt());
    }

    // --- helpers ---

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        e.setConversationId("conv-" + id);
        e.setTenantId("tenant-" + id);
        return e;
    }
}
