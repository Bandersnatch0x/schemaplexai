package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.agent.engine.entity.SfAgentExecutionLog;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolExecutionRecorderTest {

    @Mock
    private SfAgentExecutionLogMapper logMapper;

    private ToolExecutionRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ToolExecutionRecorder(logMapper);
    }

    @Test
    void recordSuccessShouldPersistToolSuccessState() {
        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 150, 42);

        recorder.record(100L, result);

        ArgumentCaptor<SfAgentExecutionLog> captor = ArgumentCaptor.forClass(SfAgentExecutionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());

        SfAgentExecutionLog log = captor.getValue();
        assertEquals(100L, log.getExecutionId());
        assertEquals("TOOL_SUCCESS", log.getState());
        assertTrue(log.getMessage().contains("tool=fileRead"));
        assertTrue(log.getMessage().contains("status=SUCCESS"));
        assertTrue(log.getMessage().contains("latencyMs=150"));
        assertTrue(log.getMessage().contains("tokens=42"));
    }

    @Test
    void recordFailureShouldPersistToolFailureStateWithCategory() {
        ToolExecutionResult result = ToolExecutionResult.failure(
            "apiCall", ToolErrorCategory.INTERNAL_ERROR, "Rate limit exceeded", 2000, 0);

        recorder.record(200L, result);

        ArgumentCaptor<SfAgentExecutionLog> captor = ArgumentCaptor.forClass(SfAgentExecutionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());

        SfAgentExecutionLog log = captor.getValue();
        assertEquals(200L, log.getExecutionId());
        assertEquals("TOOL_FAILURE", log.getState());
        assertTrue(log.getMessage().contains("tool=apiCall"));
        assertTrue(log.getMessage().contains("status=FAILURE"));
        assertTrue(log.getMessage().contains("category=INTERNAL_ERROR"));
        assertTrue(log.getMessage().contains("error=\"Rate limit exceeded\""));
        assertTrue(log.getMessage().contains("latencyMs=2000"));
        assertTrue(log.getMessage().contains("tokens=0"));
    }

    @Test
    void recordBlockedShouldPersistToolBlockedStateWithCategory() {
        ToolExecutionResult result = ToolExecutionResult.blocked(
            "volumeDelete", ToolErrorCategory.IRREVERSIBLE_OPERATION, "Irreversible operation blocked");

        recorder.record(300L, result);

        ArgumentCaptor<SfAgentExecutionLog> captor = ArgumentCaptor.forClass(SfAgentExecutionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());

        SfAgentExecutionLog log = captor.getValue();
        assertEquals(300L, log.getExecutionId());
        assertEquals("TOOL_BLOCKED", log.getState());
        assertTrue(log.getMessage().contains("tool=volumeDelete"));
        assertTrue(log.getMessage().contains("status=BLOCKED"));
        assertTrue(log.getMessage().contains("category=IRREVERSIBLE_OPERATION"));
        assertTrue(log.getMessage().contains("error=\"Irreversible operation blocked\""));
        assertTrue(log.getMessage().contains("latencyMs=0"));
        assertTrue(log.getMessage().contains("tokens=0"));
    }

    @Test
    void recordShouldNotThrowWhenMapperFailsForNonSecurityEvent() {
        doThrow(new RuntimeException("DB down")).when(logMapper).insert(any(SfAgentExecutionLog.class));

        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 100, 10);

        assertDoesNotThrow(() -> recorder.record(400L, result));

        verify(logMapper, times(1)).insert(any(SfAgentExecutionLog.class));
    }

    @Test
    void recordShouldThrowWhenMapperFailsForSecurityEvent() {
        doThrow(new RuntimeException("DB down")).when(logMapper).insert(any(SfAgentExecutionLog.class));

        ToolExecutionResult result = ToolExecutionResult.blocked(
            "volumeDelete", ToolErrorCategory.IRREVERSIBLE_OPERATION, "Blocked");

        ToolExecutionAuditException exception = assertThrows(ToolExecutionAuditException.class,
            () -> recorder.record(500L, result));

        assertTrue(exception.getMessage().contains("Security-related tool execution audit log failed"));
        verify(logMapper, times(1)).insert(any(SfAgentExecutionLog.class));
    }

    @Test
    void listRecentFailuresShouldParsePersistedFailureLogs() {
        SfAgentExecutionLog failure = new SfAgentExecutionLog();
        failure.setState("TOOL_FAILURE");
        failure.setMessage("tool=apiCall, status=FAILURE, category=RATE_LIMITED, "
                + "error=\"Rate limit exceeded\", latencyMs=2000, tokens=0");

        SfAgentExecutionLog blocked = new SfAgentExecutionLog();
        blocked.setState("TOOL_BLOCKED");
        blocked.setMessage("tool=volumeDelete, status=BLOCKED, category=IRREVERSIBLE_OPERATION, "
                + "error=\"Blocked\", latencyMs=0, tokens=0");

        when(logMapper.selectList(any())).thenReturn(List.of(failure, blocked));

        List<ToolExecutionResult> results = recorder.listRecentFailures("tenant-1", 20);

        assertEquals(2, results.size());
        assertEquals("apiCall", results.get(0).toolName());
        assertEquals(ToolErrorCategory.RATE_LIMITED, results.get(0).errorCategory());
        assertEquals("Rate limit exceeded", results.get(0).errorMessage());
        assertEquals(2000L, results.get(0).latencyMs());
        assertFalse(results.get(0).blocked());

        assertEquals("volumeDelete", results.get(1).toolName());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, results.get(1).errorCategory());
        assertTrue(results.get(1).blocked());
    }
}
