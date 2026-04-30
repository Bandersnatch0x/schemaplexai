package com.schemaplexai.ops.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfSyncBatchLog;
import com.schemaplexai.ops.entity.SfSyncCursor;
import com.schemaplexai.ops.mapper.SyncBatchLogMapper;
import com.schemaplexai.ops.mapper.SyncCursorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickHouseCostSyncService {

    private final SyncCursorMapper syncCursorMapper;
    private final SyncBatchLogMapper syncBatchLogMapper;

    @Scheduled(fixedDelay = 300000)
    @Transactional(rollbackFor = Exception.class)
    public void syncIncrementalData() {
        log.info("Start incremental sync from PG to ClickHouse");

        SfSyncBatchLog batchLog = new SfSyncBatchLog();
        batchLog.setSyncTable("sf_agent_execution");
        batchLog.setStatus("RUNNING");
        batchLog.setStartTime(LocalDateTime.now());
        syncBatchLogMapper.insert(batchLog);

        try {
            // Phase 1: Track sync cursor for incremental sync
            // Phase 2: Actual ClickHouse sync via JDBC when ClickHouse is configured
            SfSyncCursor cursor = syncCursorMapper.selectById("sf_agent_execution");
            if (cursor == null) {
                cursor = new SfSyncCursor();
                cursor.setSyncTable("sf_agent_execution");
                cursor.setLastSyncId(0L);
                cursor.setLastSyncTime(LocalDateTime.now().minusDays(1));
                syncCursorMapper.insert(cursor);
            }

            int batchSize = 1000;
            int successCount = 0;

            // Placeholder: actual sync logic would query PG and insert into ClickHouse
            log.info("Syncing from cursor id: {}, last sync time: {}",
                    cursor.getLastSyncId(), cursor.getLastSyncTime());

            // Update cursor
            cursor.setLastSyncTime(LocalDateTime.now());
            cursor.setLastSyncId(cursor.getLastSyncId() + batchSize);
            syncCursorMapper.updateById(cursor);

            batchLog.setStatus("COMPLETED");
            batchLog.setSuccessCount(successCount);
            batchLog.setBatchSize(batchSize);
            batchLog.setEndTime(LocalDateTime.now());
            syncBatchLogMapper.updateById(batchLog);

            log.info("Incremental sync completed: {} records", successCount);
        } catch (Exception e) {
            log.error("Incremental sync failed", e);
            batchLog.setStatus("FAILED");
            batchLog.setErrorMsg(e.getMessage());
            batchLog.setEndTime(LocalDateTime.now());
            syncBatchLogMapper.updateById(batchLog);
            throw new BaseException(ResultCode.SYNC_CURSOR_ERROR, "Sync failed: " + e.getMessage());
        }
    }
}
