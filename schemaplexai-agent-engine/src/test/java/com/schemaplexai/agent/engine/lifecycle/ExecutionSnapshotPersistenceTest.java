package com.schemaplexai.agent.engine.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.service.ExecutionSnapshotService;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionSnapshotPersistenceTest {

    @Mock
    private SfAgentExecutionSnapshotMapper snapshotMapper;

    @Mock
    private ExecutionSnapshotService executionSnapshotService;

    private ExecutionSnapshotPersistence persistence;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        persistence = new ExecutionSnapshotPersistence(snapshotMapper, objectMapper, executionSnapshotService);
    }

    @Test
    void saveSnapshotPersistsLegacySnapshotWithHashAndDualTierSnapshot() {
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .executionId(1L)
                .state(AgentExecutionState.THINKING)
                .build();

        persistence.saveSnapshot(snapshot);

        ArgumentCaptor<SfAgentExecutionSnapshot> captor =
                ArgumentCaptor.forClass(SfAgentExecutionSnapshot.class);
        verify(snapshotMapper).insert(captor.capture());
        SfAgentExecutionSnapshot saved = captor.getValue();

        assertEquals(1L, saved.getExecutionId());
        assertNotNull(saved.getSnapshotJson());
        assertEquals(HashUtils.sha256(saved.getSnapshotJson()), saved.getSnapshotHash());
        verify(executionSnapshotService).saveSnapshot(eq(1L), eq(saved.getSnapshotJson()), eq(0));
    }

    @Test
    void getLatestSnapshotReturnsCachedSnapshotBeforeLegacyFallback() {
        when(executionSnapshotService.restoreSnapshot(1L))
                .thenReturn("{\"executionId\":1,\"state\":\"PAUSED\",\"chatHistory\":[{\"role\":\"user\",\"content\":\"hello\"}]}");

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
        assertEquals(AgentExecutionState.PAUSED, result.getState());
        assertEquals(1, result.getChatHistory().size());
        verifyNoInteractions(snapshotMapper);
    }

    @Test
    void getLatestSnapshotFallsBackToLegacySnapshotWithValidHash() {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";
        SfAgentExecutionSnapshot entity = legacySnapshot(snapshotJson, HashUtils.sha256(snapshotJson));
        when(executionSnapshotService.restoreSnapshot(1L)).thenReturn(null);
        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
        assertEquals(AgentExecutionState.PAUSED, result.getState());
    }

    @Test
    void getLatestSnapshotAllowsLegacySnapshotWithoutHash() {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";
        SfAgentExecutionSnapshot entity = legacySnapshot(snapshotJson, null);
        when(executionSnapshotService.restoreSnapshot(1L)).thenReturn(null);
        when(snapshotMapper.selectOne(any())).thenReturn(entity);

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
    }

    @Test
    void getLatestSnapshotReturnsNullWhenNoSnapshotExists() {
        when(executionSnapshotService.restoreSnapshot(1L)).thenReturn(null);
        when(snapshotMapper.selectOne(any())).thenReturn(null);

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNull(result);
    }

    @Test
    void getLatestSnapshotReturnsNullOnLegacyDeserializeError() {
        when(executionSnapshotService.restoreSnapshot(1L)).thenReturn(null);
        when(snapshotMapper.selectOne(any())).thenReturn(legacySnapshot("not-valid-json", null));

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNull(result);
    }

    @Test
    void getLatestSnapshotReturnsNullWhenLegacyHashDoesNotMatch() {
        String snapshotJson = "{\"executionId\":1,\"state\":\"PAUSED\"}";
        when(executionSnapshotService.restoreSnapshot(1L)).thenReturn(null);
        when(snapshotMapper.selectOne(any())).thenReturn(legacySnapshot(snapshotJson, "0".repeat(64)));

        ExecutionSnapshot result = persistence.getLatestSnapshot(1L);

        assertNull(result);
    }

    private static SfAgentExecutionSnapshot legacySnapshot(String snapshotJson, String snapshotHash) {
        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(1L);
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotHash(snapshotHash);
        return entity;
    }
}
