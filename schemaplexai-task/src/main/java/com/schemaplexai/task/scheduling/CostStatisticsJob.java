package com.schemaplexai.task.scheduling;

import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.CostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean({CostDataSyncService.class, CostService.class})
@RequiredArgsConstructor
public class CostStatisticsJob {

    private final CostDataSyncService costDataSyncService;
    private final CostService costService;

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "costStatisticsJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[CostStatisticsJob] Start cost statistics job");
        try {
            costDataSyncService.syncIncrementalData();
            costService.checkBudgetAlerts();
            log.info("[CostStatisticsJob] Cost statistics completed");
        } catch (Exception e) {
            log.error("[CostStatisticsJob] Cost statistics failed", e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Cost statistics job failed", e);
        }
    }
}
