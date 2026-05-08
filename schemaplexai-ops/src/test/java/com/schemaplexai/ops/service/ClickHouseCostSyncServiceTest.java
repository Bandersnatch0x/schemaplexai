package com.schemaplexai.ops.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfSyncBatchLog;
import com.schemaplexai.ops.entity.SfSyncCursor;
import com.schemaplexai.ops.mapper.SyncBatchLogMapper;
import com.schemaplexai.ops.mapper.SyncCursorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickHouseCostSyncServiceTest {

    @Mock
    private SyncCursorMapper syncCursorMapper;

    @Mock
    private SyncBatchLogMapper syncBatchLogMapper;

    @Mock
    private JdbcTemplate pgJdbcTemplate;

    @Mock
    private DataSource clickHouseDataSource;

    @Mock
    private Connection clickHouseConnection;

    @Mock
    private PreparedStatement clickHousePreparedStatement;

    private ClickHouseCostSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new ClickHouseCostSyncService(
                syncCursorMapper,
                syncBatchLogMapper,
                pgJdbcTemplate,
                clickHouseDataSource,
                true
        );
    }

    @Test
    void syncIncrementalData_disabled_skipsSync() {
        ClickHouseCostSyncService disabledService = new ClickHouseCostSyncService(
                syncCursorMapper, syncBatchLogMapper, pgJdbcTemplate, clickHouseDataSource, false
        );

        disabledService.syncIncrementalData();

        verifyNoInteractions(syncCursorMapper, syncBatchLogMapper, pgJdbcTemplate);
    }

    @Test
    void syncIncrementalData_noRecords_completesWithZero() {
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        SfSyncCursor existingCursor = new SfSyncCursor();
        existingCursor.setSyncTable("sf_agent_execution");
        existingCursor.setLastSyncId(100L);
        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(existingCursor);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        syncService.syncIncrementalData();

        verify(syncBatchLogMapper).insert(any(SfSyncBatchLog.class));
        verify(syncBatchLogMapper).updateById(any(SfSyncBatchLog.class));
        verify(syncCursorMapper, never()).updateById(any(SfSyncCursor.class));
    }

    @Test
    void syncIncrementalData_newCursor_createdWithDefaultValues() {
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(null);
        when(syncCursorMapper.insert(any(SfSyncCursor.class))).thenReturn(1);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        syncService.syncIncrementalData();

        ArgumentCaptor<SfSyncCursor> cursorCaptor = ArgumentCaptor.forClass(SfSyncCursor.class);
        verify(syncCursorMapper).insert(cursorCaptor.capture());
        SfSyncCursor createdCursor = cursorCaptor.getValue();
        assertThat(createdCursor.getSyncTable()).isEqualTo("sf_agent_execution");
        assertThat(createdCursor.getLastSyncId()).isEqualTo(0L);
        assertThat(createdCursor.getLastSyncTime()).isNotNull();
    }

    @Test
    void syncIncrementalData_withRecords_syncsAndAdvancesCursor() throws Exception {
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        SfSyncCursor cursor = new SfSyncCursor();
        cursor.setSyncTable("sf_agent_execution");
        cursor.setLastSyncId(0L);
        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(cursor);
        when(syncCursorMapper.updateById(any(SfSyncCursor.class))).thenReturn(1);

        Object record = createExecutionRecord(1L, 10L, "conv-1", "COMPLETED",
                LocalDateTime.now(), 100L, 1L,
                LocalDateTime.now(), LocalDateTime.now(), 1L, 1L, false);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(record));

        when(clickHouseDataSource.getConnection()).thenReturn(clickHouseConnection);
        when(clickHouseConnection.prepareStatement(anyString())).thenReturn(clickHousePreparedStatement);
        when(clickHousePreparedStatement.executeBatch()).thenReturn(new int[]{1});

        syncService.syncIncrementalData();

        verify(clickHousePreparedStatement).addBatch();
        verify(clickHousePreparedStatement).executeBatch();
        verify(syncCursorMapper).updateById(any(SfSyncCursor.class));
        verify(syncBatchLogMapper, times(1)).updateById(any(SfSyncBatchLog.class));
    }

    @Test
    void syncIncrementalData_clickHouseConnectionFailure_throwsBaseException() throws Exception {
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        SfSyncCursor cursor = new SfSyncCursor();
        cursor.setSyncTable("sf_agent_execution");
        cursor.setLastSyncId(0L);
        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(cursor);
        lenient().when(syncBatchLogMapper.updateById(any(SfSyncBatchLog.class))).thenReturn(1);

        Object record = createExecutionRecord(1L, 10L, "conv-1", "COMPLETED",
                null, 100L, 1L, null, null, 1L, 1L, false);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(record));

        when(clickHouseDataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        assertThatThrownBy(() -> syncService.syncIncrementalData())
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.SYNC_CURSOR_ERROR.getCode());
                    assertThat(be.getMessage()).contains("ClickHouse insert failed");
                });
    }

    @Test
    void syncIncrementalData_batchLogCreatedWithRunningStatus() {
        final SfSyncBatchLog[] capturedBatchLog = new SfSyncBatchLog[1];
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            capturedBatchLog[0] = new SfSyncBatchLog();
            capturedBatchLog[0].setSyncTable(log.getSyncTable());
            capturedBatchLog[0].setStatus(log.getStatus());
            capturedBatchLog[0].setStartTime(log.getStartTime());
            log.setId(1L);
            return 1;
        });

        SfSyncCursor cursor = new SfSyncCursor();
        cursor.setSyncTable("sf_agent_execution");
        cursor.setLastSyncId(0L);
        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(cursor);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        syncService.syncIncrementalData();

        assertThat(capturedBatchLog[0]).isNotNull();
        assertThat(capturedBatchLog[0].getSyncTable()).isEqualTo("sf_agent_execution");
        assertThat(capturedBatchLog[0].getStatus()).isEqualTo("RUNNING");
        assertThat(capturedBatchLog[0].getStartTime()).isNotNull();
    }

    @Test
    void syncIncrementalData_cursorAdvancedToMaxId() throws Exception {
        when(syncBatchLogMapper.insert(any(SfSyncBatchLog.class))).thenAnswer(inv -> {
            SfSyncBatchLog log = inv.getArgument(0);
            log.setId(1L);
            return 1;
        });

        SfSyncCursor cursor = new SfSyncCursor();
        cursor.setSyncTable("sf_agent_execution");
        cursor.setLastSyncId(10L);
        when(syncCursorMapper.selectById("sf_agent_execution")).thenReturn(cursor);
        when(syncCursorMapper.updateById(any(SfSyncCursor.class))).thenReturn(1);

        Object record1 = createExecutionRecord(11L, 1L, "c1", "DONE",
                LocalDateTime.now(), 1L, 1L,
                LocalDateTime.now(), LocalDateTime.now(), 1L, 1L, false);
        Object record2 = createExecutionRecord(15L, 2L, "c2", "DONE",
                LocalDateTime.now(), 1L, 1L,
                LocalDateTime.now(), LocalDateTime.now(), 1L, 1L, false);

        when(pgJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(record1, record2));

        when(clickHouseDataSource.getConnection()).thenReturn(clickHouseConnection);
        when(clickHouseConnection.prepareStatement(anyString())).thenReturn(clickHousePreparedStatement);
        when(clickHousePreparedStatement.executeBatch()).thenReturn(new int[]{1, 1});

        syncService.syncIncrementalData();

        ArgumentCaptor<SfSyncCursor> cursorCaptor = ArgumentCaptor.forClass(SfSyncCursor.class);
        verify(syncCursorMapper).updateById(cursorCaptor.capture());
        assertThat(cursorCaptor.getValue().getLastSyncId()).isEqualTo(15L);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Object createExecutionRecord(long id, long agentId, String conversationId, String state,
                                          LocalDateTime completedAt, long snapshotId, long tenantId,
                                          LocalDateTime createdAt, LocalDateTime updatedAt,
                                          long createdBy, long updatedBy, boolean deleted) throws Exception {
        Class<?> recordClass = Class.forName("com.schemaplexai.ops.service.ClickHouseCostSyncService$ExecutionRecord");
        Constructor<?> ctor = recordClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Pass all args as boxed types to avoid varargs primitive boxing issues
        return ctor.newInstance(
                Long.valueOf(id),
                Long.valueOf(agentId),
                conversationId,
                state,
                completedAt,
                Long.valueOf(snapshotId),
                Long.valueOf(tenantId),
                createdAt,
                updatedAt,
                Long.valueOf(createdBy),
                Long.valueOf(updatedBy),
                Boolean.valueOf(deleted)
        );
    }
}
