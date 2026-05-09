package com.schemaplexai.agent.engine.external.codex;

import com.schemaplexai.agent.engine.external.AgentEvent;
import com.schemaplexai.agent.engine.external.ExternalAgentConfig;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodexAdapterTest {

    private ExternalAgentConfig config;
    private CodexAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new ExternalAgentConfig();
        config.setEnabled(true);
        config.setTimeoutMs(5000);
        adapter = new CodexAdapter(config);
    }

    @Test
    void startSession_shouldCreateSession() {
        adapter.startSession("session-1");

        List<AgentEvent> events = adapter.getEvents("session-1");
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void startSession_shouldThrowWhenSessionIdIsNull() {
        BaseException ex = assertThrows(BaseException.class, () -> adapter.startSession(null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void startSession_shouldThrowWhenSessionIdIsBlank() {
        BaseException ex = assertThrows(BaseException.class, () -> adapter.startSession("   "));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void startSession_shouldNotDuplicateWhenCalledTwice() {
        adapter.startSession("session-dup");
        adapter.startSession("session-dup"); // should log warning but not throw

        List<AgentEvent> events = adapter.getEvents("session-dup");
        assertNotNull(events);
    }

    @Test
    void sendMessage_shouldThrowWhenNotEnabled() {
        config.setEnabled(false);
        adapter.startSession("session-2");

        BaseException ex = assertThrows(BaseException.class,
                () -> adapter.sendMessage("session-2", "hello"));
        assertEquals(ResultCode.AGENT_EXECUTION_FAILED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("disabled"));
    }

    @Test
    void sendMessage_shouldThrowWhenSessionNotFound() {
        config.setEnabled(true);

        BaseException ex = assertThrows(BaseException.class,
                () -> adapter.sendMessage("nonexistent", "hello"));
        assertEquals(ResultCode.AGENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_shouldThrowWhenMessageIsNull() {
        adapter.startSession("session-3");

        BaseException ex = assertThrows(BaseException.class,
                () -> adapter.sendMessage("session-3", null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_shouldThrowWhenMessageIsBlank() {
        adapter.startSession("session-4");

        BaseException ex = assertThrows(BaseException.class,
                () -> adapter.sendMessage("session-4", "   "));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void getEvents_shouldReturnEmptyListForUnknownSession() {
        List<AgentEvent> events = adapter.getEvents("unknown");
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void getEvents_shouldReturnEmptyListForNullSessionId() {
        List<AgentEvent> events = adapter.getEvents(null);
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void terminateSession_shouldRemoveSession() {
        adapter.startSession("session-5");
        adapter.terminateSession("session-5");

        List<AgentEvent> events = adapter.getEvents("session-5");
        assertTrue(events.isEmpty());
    }

    @Test
    void terminateSession_shouldNotThrowForUnknownSession() {
        assertDoesNotThrow(() -> adapter.terminateSession("unknown-session"));
    }

    @Test
    void fullLifecycle_shouldWork() {
        String sessionId = "lifecycle-session";
        adapter.startSession(sessionId);
        assertTrue(adapter.getEvents(sessionId).isEmpty());

        // sendMessage may succeed or fail depending on whether "codex" CLI is installed.
        // We only verify that the call completes (either path) and events are tracked.
        try {
            AgentEvent event = adapter.sendMessage(sessionId, "hello");
            assertNotNull(event);
            assertTrue(event.getType().equals("response") || event.getType().equals("error"));
        } catch (BaseException e) {
            // Expected when codex CLI is not available — verify meaningful error
            assertNotNull(e.getCode());
        }

        adapter.terminateSession(sessionId);
        assertTrue(adapter.getEvents(sessionId).isEmpty());
    }
}
