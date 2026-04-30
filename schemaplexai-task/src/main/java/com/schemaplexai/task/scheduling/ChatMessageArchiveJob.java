package com.schemaplexai.task.scheduling;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatMessageArchiveJob {

    @Scheduled(cron = "0 30 1 * * ?")
    @SchedulerLock(name = "chatMessageArchiveJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[ChatMessageArchiveJob] Start chat message archive");
        try {
            // TODO: archive expired chat messages to cold storage
            log.info("[ChatMessageArchiveJob] Chat message archive completed");
        } catch (Exception e) {
            log.error("[ChatMessageArchiveJob] Chat message archive failed", e);
        }
    }
}
