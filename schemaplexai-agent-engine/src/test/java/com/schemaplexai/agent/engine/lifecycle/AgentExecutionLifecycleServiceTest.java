package com.schemaplexai.agent.engine.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentExecutionLifecycleServiceTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private SfAgentExecutionSnapshotMapper snapshotMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ObjectMapper objectMapper;

    private AgentExecutionLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        lifecycleService = new AgentExecutionLifecycleService(
                stateMachine, executionMapper, snapshotMapper, redisTemplate, objectMapper);
    }

    @Test
    void getLatestSnapshotReturnsDeserializedSnapshot() {
        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson("{\"executionId\":1,\"state\":\"PAUSED\",\"chatHistory\":[{\"role\":\"user\",\"content\":\"hello\"}]}");

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
        assertEquals(AgentExecutionState.PAUSED, result.getState());
        assertEquals(1, result.getChatHistory().size());
    }

    @Test
    void getLatestSnapshotReturnsNullWhenNoSnapshot() {
        when(snapshotMapper.selectOne(any())).thenReturn(null);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNull(result);
    }

    @Test
    void getLatestSnapshotReturnsNullOnDeserializeError() {
        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson("not-valid-json");

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNull(result);
    }

    @Test
    void pauseExecutionSetsRedisAndTransitionsState() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.pauseExecution(1L, PauseReason.USER_REQUEST);

        verify(valueOps).set(anyString(), eq("USER_REQUEST"), any());
        verify(stateMachine).transition(AgentExecutionState.PAUSED, execution);
    }

    @Test
    void resumeExecutionClearsRedisAndTransitionsToReady() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.resumeExecution(1L);

        verify(redisTemplate).delete(anyString());
        verify(stateMachine).transition(AgentExecutionState.READY, execution);
    }

    @Test
    void cancelExecutionRemovesStateAndTransitionsToCancelled() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        lifecycleService.cancelExecution(1L);

        verify(redisTemplate).delete(anyString());
        verify(stateMachine).transition(AgentExecutionState.CANCELLED, execution);
        verify(stateMachine).removeExecution(1L);
    }

    @Test
    void saveSnapshotPersistsSerializedJson() {
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.THINKING)
                .build();

        lifecycleService.saveSnapshot(snapshot);

        verify(snapshotMapper).insert(any(SfAgentExecutionSnapshot.class));
    }

    @Test
    void saveAndRetrieveSnapshot_withLocalDateTime() {
        LocalDateTime now = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.THINKING)
                .createdAt(now)
                .build();

        lifecycleService.saveSnapshot(snapshot);

        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson("{\"executionId\":1,\"state\":\"THINKING\",\"createdAt\":\"2024-06-15T10:30:00\"}");

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot retrieved = lifecycleService.getLatestSnapshot(1L);

        assertNotNull(retrieved);
        assertEquals(now, retrieved.getCreatedAt());
    }

    // -------------------------------------------------------------------------
    // Hash integrity tests
    // -------------------------------------------------------------------------

    @Test
    void saveSnapshot_computesHash() throws Exception {
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.THINKING)
                .build();

        lifecycleService.saveSnapshot(snapshot);

        ArgumentCaptor<SfAgentExecutionSnapshot> captor = ArgumentCaptor.forClass(SfAgentExecutionSnapshot.class);
        verify(snapshotMapper).insert(captor.capture());
        SfAgentExecutionSnapshot saved = captor.getValue();

        assertNotNull(saved.getSnapshotHash());
        assertEquals(64, saved.getSnapshotHash().length());

        // Verify hash matches recomputed value
        String expectedHash = computeSha256(saved.getSnapshotJson());
        assertEquals(expectedHash, saved.getSnapshotHash());
    }

    @Test
    void getLatestSnapshot_validHash_returnsSnapshot() throws Exception {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";
        String validHash = computeSha256(snapshotJson);

        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotHash(validHash);

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
        assertEquals(AgentExecutionState.PAUSED, result.getState());
    }

    @Test
    void getLatestSnapshot_invalidHash_returnsNull() throws Exception {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";

        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotHash("0000000000000000000000000000000000000000000000000000000000000000");

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNull(result);
    }

    @Test
    void getLatestSnapshot_nullHash_logsWarning() throws Exception {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";

        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotHash(null);

        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = lifecycleService.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
    }

    private static String computeSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(64);
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
