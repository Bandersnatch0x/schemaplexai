package com.schemaplexai.task.scheduling;

import com.schemaplexai.task.service.MilvusReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusReconciliationJob {

    private final MilvusReconciliationService reconciliationService;

    @Value("${task.milvus-reconciliation.batch-size:100}")
    private int batchSize = 100;

    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "milvusReconciliationJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[MilvusReconciliationJob] Start Milvus reconciliation");
        try {
            int dispatched = reconciliationService.reconcilePendingDocuments(batchSize);
            log.info("[MilvusReconciliationJob] Milvus reconciliation completed, dispatched={}", dispatched);
        } catch (Exception e) {
            log.error("[MilvusReconciliationJob] Milvus reconciliation failed", e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Milvus reconciliation job failed", e);
        }
    }
}
