package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.agent.engine.entity.SfAgentExecutionLog;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolExecutionRecorderTest {

    @Mock
    private SfAgentExecutionLogMapper logMapper;

    @InjectMocks
    private ToolExecutionRecorder recorder;

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
            "apiCall", ToolErrorCategory.PROVIDER_ERROR, "Rate limit exceeded", 2000, 0);

        recorder.record(200L, result);

        ArgumentCaptor<SfAgentExecutionLog> captor = ArgumentCaptor.forClass(SfAgentExecutionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());

        SfAgentExecutionLog log = captor.getValue();
        assertEquals(200L, log.getExecutionId());
        assertEquals("TOOL_FAILURE", log.getState());
        assertTrue(log.getMessage().contains("tool=apiCall"));
        assertTrue(log.getMessage().contains("status=FAILURE"));
        assertTrue(log.getMessage().contains("category=PROVIDER_ERROR"));
        assertTrue(log.getMessage().contains("error=\"Rate limit exceeded\""));
        assertTrue(log.getMessage().contains("latencyMs=2000"));
        assertTrue(log.getMessage().contains("tokens=0"));
    }

    @Test
    void recordBlockedShouldPersistToolBlockedStateWithCategory() {
        ToolExecutionResult result = ToolExecutionResult.blocked(
            "volumeDelete", ToolErrorCategory.UNAUTHORIZED_SCOPE, "Irreversible operation blocked");

        recorder.record(300L, result);

        ArgumentCaptor<SfAgentExecutionLog> captor = ArgumentCaptor.forClass(SfAgentExecutionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());

        SfAgentExecutionLog log = captor.getValue();
        assertEquals(300L, log.getExecutionId());
        assertEquals("TOOL_BLOCKED", log.getState());
        assertTrue(log.getMessage().contains("tool=volumeDelete"));
        assertTrue(log.getMessage().contains("status=BLOCKED"));
        assertTrue(log.getMessage().contains("category=UNAUTHORIZED_SCOPE"));
        assertTrue(log.getMessage().contains("error=\"Irreversible operation blocked\""));
        assertTrue(log.getMessage().contains("latencyMs=0"));
        assertTrue(log.getMessage().contains("tokens=0"));
    }

    @Test
    void recordShouldNotThrowWhenMapperFails() {
        doThrow(new RuntimeException("DB down")).when(logMapper).insert(any(SfAgentExecutionLog.class));

        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 100, 10);

        assertDoesNotThrow(() -> recorder.record(400L, result));

        verify(logMapper, times(1)).insert(any(SfAgentExecutionLog.class));
    }
}
