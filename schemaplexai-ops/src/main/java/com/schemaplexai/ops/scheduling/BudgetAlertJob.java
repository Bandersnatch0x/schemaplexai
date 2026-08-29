package com.schemaplexai.ops.scheduling;

import com.schemaplexai.ops.service.CostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly budget alert check (issue 921 / REQ-09).
 * <p>
 * The cost-analytics spec §3.3/§5 requires the budget alert check to run every
 * hour ({@code BudgetAlertJob | 每小时 | 预算告警检查}); previously it only ran as a
 * tail step of the daily 01:00 CostStatisticsJob, delaying breach detection by up
 * to 24h. Alert dispatch (notification persistence + MQ notification chain) is
 * wired inside {@link CostService#checkBudgetAlerts()}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertJob {

    /** Spec §3.3: {@code @Scheduled(cron = "0 0 * * * ?") // 每小时}. */
    public static final String HOURLY_CRON = "0 0 * * * ?";

    private final CostService costService;

    @Scheduled(cron = HOURLY_CRON)
    public void run() {
        log.info("[BudgetAlertJob] Start hourly budget alert check");
        try {
            costService.checkBudgetAlerts();
            log.info("[BudgetAlertJob] Budget alert check completed");
        } catch (Exception e) {
            // A bad budget row (e.g. zero limit) must not kill the schedule;
            // the next hourly run retries.
            log.error("[BudgetAlertJob] Budget alert check failed", e);
        }
    }
}
