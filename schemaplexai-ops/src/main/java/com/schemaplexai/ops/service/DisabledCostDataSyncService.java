package com.schemaplexai.ops.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "clickhouse.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledCostDataSyncService implements CostDataSyncService {

    @Override
    public void syncIncrementalData() {
        log.debug("ClickHouse cost sync is disabled. Skipping incremental sync.");
    }
}
