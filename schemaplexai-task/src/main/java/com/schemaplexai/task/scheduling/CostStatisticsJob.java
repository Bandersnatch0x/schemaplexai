package com.schemaplexai.task.scheduling;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CostStatisticsJob {

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "costStatisticsJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[CostStatisticsJob] Start cost statistics job");
        try {
            // Phase 1: Placeholder — actual cost statistics from ClickHouse to be implemented
            // This job should query cost data and update budget alerts
            log.info("[CostStatisticsJob] Cost statistics completed");
        } catch (Exception e) {
            log.error("[CostStatisticsJob] Cost statistics failed", e);
        }
    }
}
