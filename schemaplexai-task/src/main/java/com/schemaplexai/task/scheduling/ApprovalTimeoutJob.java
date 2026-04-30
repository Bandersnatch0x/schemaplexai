package com.schemaplexai.task.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTimeoutJob {

    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "approvalTimeoutJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[ApprovalTimeoutJob] Start approval timeout check");
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(24);
            log.info("[ApprovalTimeoutJob] Checking approvals older than {}", timeoutThreshold);
            // Phase 1: Log timeout threshold
            // Phase 2: Query pending approvals, auto-reject or escalate expired ones
            log.info("[ApprovalTimeoutJob] Approval timeout check completed");
        } catch (Exception e) {
            log.error("[ApprovalTimeoutJob] Approval timeout check failed", e);
        }
    }
}
