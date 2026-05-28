package com.schemaplexai.task.scheduling;

import com.schemaplexai.quality.service.EscalationPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(EscalationPolicyService.class)
@RequiredArgsConstructor
public class ApprovalTimeoutJob {

    private final EscalationPolicyService escalationPolicyService;

    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "approvalTimeoutJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[ApprovalTimeoutJob] Start approval timeout check");
        try {
            escalationPolicyService.checkEscalations();
            log.info("[ApprovalTimeoutJob] Approval timeout check completed");
        } catch (Exception e) {
            log.error("[ApprovalTimeoutJob] Approval timeout check failed", e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Approval timeout job failed", e);
        }
    }
}
