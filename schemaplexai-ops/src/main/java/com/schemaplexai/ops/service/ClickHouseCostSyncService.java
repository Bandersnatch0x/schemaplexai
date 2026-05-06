package com.schemaplexai.ops.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfSyncBatchLog;
import com.schemaplexai.ops.entity.SfSyncCursor;
import com.schemaplexai.ops.mapper.SyncBatchLogMapper;
import com.schemaplexai.ops.mapper.SyncCursorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClickHouseCostSyncService {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CURSOR_KEY = "sf_agent_execution";

    private final SyncCursorMapper syncCursorMapper;
    private final SyncBatchLogMapper syncBatchLogMapper;
    private final JdbcTemplate pgJdbcTemplate;
    private final DataSource clickHouseDataSource;

    public ClickHouseCostSyncService(
            SyncCursorMapper syncCursorMapper,
            SyncBatchLogMapper syncBatchLogMapper,
            JdbcTemplate pgJdbcTemplate,
            @Qualifier("clickHouseDataSource") DataSource clickHouseDataSource) {
        this.syncCursorMapper = syncCursorMapper;
        this.syncBatchLogMapper = syncBatchLogMapper;
        this.pgJdbcTemplate = pgJdbcTemplate;
        this.clickHouseDataSource = clickHouseDataSource;
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional(rollbackFor = Exception.class)
    public void syncIncrementalData() {
        log.debug("Starting incremental sync from PG to ClickHouse");

        SfSyncBatchLog batchLog = createBatchLog();
        SfSyncCursor cursor = getOrCreateCursor();
        int batchSize = DEFAULT_BATCH_SIZE;
        int successCount = 0;
        int failCount = 0;

        try {
            List<ExecutionRecord> records = fetchExecutionRecords(cursor.getLastSyncId(), batchSize);

            if (records.isEmpty()) {
                log.debug("No new records to sync. Cursor at id: {}", cursor.getLastSyncId());
                completeBatchLog(batchLog, 0, 0);
                return;
            }

            successCount = insertIntoClickHouse(records);
            failCount = records.size() - successCount;

            Long maxId = records.stream()
                    .mapToLong(ExecutionRecord::id)
                    .max()
                    .orElse(cursor.getLastSyncId());

            updateCursor(cursor, maxId);
            completeBatchLog(batchLog, successCount, failCount);

            log.info("Incremental sync completed: {} synced, {} failed, cursor advanced to id: {}",
                    successCount, failCount, maxId);
        } catch (Exception e) {
            log.error("Incremental sync failed at cursor id: {}", cursor.getLastSyncId(), e);
            failBatchLog(batchLog, e.getMessage());
            throw new BaseException(ResultCode.SYNC_CURSOR_ERROR,
                    "Sync failed at cursor " + cursor.getLastSyncId() + ": " + e.getMessage());
        }
    }

    private SfSyncBatchLog createBatchLog() {
        SfSyncBatchLog batchLog = new SfSyncBatchLog();
        batchLog.setSyncTable(CURSOR_KEY);
        batchLog.setStatus("RUNNING");
        batchLog.setStartTime(LocalDateTime.now());
        syncBatchLogMapper.insert(batchLog);
        return batchLog;
    }

    private SfSyncCursor getOrCreateCursor() {
        SfSyncCursor cursor = syncCursorMapper.selectById(CURSOR_KEY);
        if (cursor == null) {
            cursor = new SfSyncCursor();
            cursor.setSyncTable(CURSOR_KEY);
            cursor.setLastSyncId(0L);
            cursor.setLastSyncTime(LocalDateTime.now().minusDays(1));
            syncCursorMapper.insert(cursor);
        }
        return cursor;
    }

    private List<ExecutionRecord> fetchExecutionRecords(Long lastSyncId, int batchSize) {
        String sql = """
                SELECT id, agent_id, conversation_id, state, completed_at, snapshot_id,
                       tenant_id, created_at, updated_at, created_by, updated_by, deleted
                FROM sf_agent_execution
                WHERE id > ?
                ORDER BY id ASC
                LIMIT ?
                """;

        return pgJdbcTemplate.query(sql,
                ps -> {
                    ps.setLong(1, lastSyncId);
                    ps.setInt(2, batchSize);
                },
                (rs, rowNum) -> mapToRecord(rs));
    }

    private ExecutionRecord mapToRecord(ResultSet rs) throws SQLException {
        return new ExecutionRecord(
                rs.getLong("id"),
                rs.getLong("agent_id"),
                rs.getString("conversation_id"),
                rs.getString("state"),
                rs.getTimestamp("completed_at") != null
                        ? rs.getTimestamp("completed_at").toLocalDateTime() : null,
                rs.getLong("snapshot_id"),
                rs.getLong("tenant_id"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                rs.getLong("created_by"),
                rs.getLong("updated_by"),
                rs.getBoolean("deleted")
        );
    }

    private int insertIntoClickHouse(List<ExecutionRecord> records) {
        String sql = """
                INSERT INTO sf_agent_execution
                (id, agent_id, conversation_id, state, completed_at, snapshot_id,
                 tenant_id, created_at, updated_at, created_by, updated_by, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int inserted = 0;
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (ExecutionRecord record : records) {
                setInsertParams(ps, record);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            for (int result : results) {
                if (result >= 0 || result == PreparedStatement.SUCCESS_NO_INFO) {
                    inserted++;
                }
            }
        } catch (SQLException e) {
            log.error("ClickHouse batch insert failed", e);
            throw new BaseException(ResultCode.SYNC_CURSOR_ERROR,
                    "ClickHouse insert failed: " + e.getMessage());
        }

        return inserted;
    }

    private void setInsertParams(PreparedStatement ps, ExecutionRecord r) throws SQLException {
        int idx = 1;
        ps.setLong(idx++, r.id());
        ps.setLong(idx++, r.agentId());
        ps.setString(idx++, r.conversationId());
        ps.setString(idx++, r.state());
        ps.setTimestamp(idx++, r.completedAt() != null
                ? Timestamp.valueOf(r.completedAt()) : null);
        ps.setObject(idx++, r.snapshotId() != 0 ? r.snapshotId() : null);
        ps.setLong(idx++, r.tenantId());
        ps.setTimestamp(idx++, r.createdAt() != null
                ? Timestamp.valueOf(r.createdAt()) : null);
        ps.setTimestamp(idx++, r.updatedAt() != null
                ? Timestamp.valueOf(r.updatedAt()) : null);
        ps.setLong(idx++, r.createdBy() != 0 ? r.createdBy() : null);
        ps.setLong(idx++, r.updatedBy() != 0 ? r.updatedBy() : null);
        ps.setBoolean(idx, r.deleted());
    }

    private void updateCursor(SfSyncCursor cursor, Long maxId) {
        cursor.setLastSyncId(maxId);
        cursor.setLastSyncTime(LocalDateTime.now());
        syncCursorMapper.updateById(cursor);
    }

    private void completeBatchLog(SfSyncBatchLog batchLog, int successCount, int failCount) {
        batchLog.setStatus("COMPLETED");
        batchLog.setSuccessCount(successCount);
        batchLog.setFailCount(failCount);
        batchLog.setBatchSize(successCount + failCount);
        batchLog.setEndTime(LocalDateTime.now());
        syncBatchLogMapper.updateById(batchLog);
    }

    private void failBatchLog(SfSyncBatchLog batchLog, String errorMsg) {
        batchLog.setStatus("FAILED");
        batchLog.setErrorMsg(errorMsg);
        batchLog.setEndTime(LocalDateTime.now());
        syncBatchLogMapper.updateById(batchLog);
    }

    /**
     * Immutable record representing a row from sf_agent_execution in PostgreSQL.
     */
    private record ExecutionRecord(
            Long id,
            Long agentId,
            String conversationId,
            String state,
            LocalDateTime completedAt,
            Long snapshotId,
            Long tenantId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long createdBy,
            Long updatedBy,
            Boolean deleted
    ) {}
}
