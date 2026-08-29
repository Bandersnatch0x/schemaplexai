package com.schemaplexai.agent.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.ExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.ExecutionSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.1: Execution Snapshot Service Tests")
class ExecutionSnapshotServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ExecutionSnapshotMapper snapshotMapper;

    @InjectMocks
    private ExecutionSnapshotService executionSnapshotService;

    private static final Long EXECUTION_ID = 42L;
    private static final String STATE_JSON = "{\"state\":\"RUNNING\",\"step\":3}";
    private static final Integer VERSION = 7;
    private static final String REDIS_KEY = "execution:snapshot:42";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("saveSnapshot writes to Redis with TTL 1 hour")
    void saveSnapshotWritesToRedis() {
        executionSnapshotService.saveSnapshot(EXECUTION_ID, STATE_JSON, VERSION);

        verify(valueOps).set(eq(REDIS_KEY), eq(STATE_JSON), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("saveSnapshot async writes to PostgreSQL via mapper")
    void saveSnapshotAsyncWritesToPg() {
        executionSnapshotService.saveSnapshot(EXECUTION_ID, STATE_JSON, VERSION);

        ArgumentCaptor<ExecutionSnapshot> captor = ArgumentCaptor.forClass(ExecutionSnapshot.class);
        verify(snapshotMapper).insert(captor.capture());
        ExecutionSnapshot saved = captor.getValue();
        assertThat(saved.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(saved.getStateJson()).isEqualTo(STATE_JSON);
        assertThat(saved.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("restoreSnapshot prefers Redis over PG when Redis hit")
    void restoreSnapshotPrefersRedis() {
        when(valueOps.get(REDIS_KEY)).thenReturn(STATE_JSON);

        String result = executionSnapshotService.restoreSnapshot(EXECUTION_ID);

        assertThat(result).isEqualTo(STATE_JSON);
        verify(snapshotMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("restoreSnapshot falls back to PG when Redis miss")
    void restoreSnapshotFallsBackToPg() {
        when(valueOps.get(REDIS_KEY)).thenReturn(null);

        ExecutionSnapshot dbSnapshot = new ExecutionSnapshot();
        dbSnapshot.setExecutionId(EXECUTION_ID);
        dbSnapshot.setStateJson(STATE_JSON);
        dbSnapshot.setVersion(VERSION);
        when(snapshotMapper.selectLatestByExecutionId(EXECUTION_ID)).thenReturn(dbSnapshot);

        String result = executionSnapshotService.restoreSnapshot(EXECUTION_ID);

        assertThat(result).isEqualTo(STATE_JSON);
        verify(valueOps).get(REDIS_KEY);
        verify(snapshotMapper).selectLatestByExecutionId(EXECUTION_ID);
    }

    @Test
    @DisplayName("restoreSnapshot returns null when both stores miss")
    void restoreSnapshotReturnsNullWhenBothMiss() {
        when(valueOps.get(REDIS_KEY)).thenReturn(null);
        when(snapshotMapper.selectLatestByExecutionId(EXECUTION_ID)).thenReturn(null);

        String result = executionSnapshotService.restoreSnapshot(EXECUTION_ID);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("deleteSnapshot removes from both Redis and PG")
    void deleteSnapshotRemovesFromBothStores() {
        executionSnapshotService.deleteSnapshot(EXECUTION_ID);

        verify(redisTemplate).delete(REDIS_KEY);
        verify(snapshotMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("NEW-02: saveSnapshot dispatches persistence through the proxy, not this")
    void saveSnapshotDispatchesPersistenceThroughSelfProxy() {
        ExecutionSnapshotService proxy = mock(ExecutionSnapshotService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(executionSnapshotService, "self", proxy);

        executionSnapshotService.saveSnapshot(EXECUTION_ID, STATE_JSON, VERSION);

        verify(proxy).persistToDatabase(EXECUTION_ID, STATE_JSON, VERSION);
        // Direct mapper insert would mean the this.-path ran synchronously instead.
        verify(snapshotMapper, never()).insert(any(ExecutionSnapshot.class));
    }
}
