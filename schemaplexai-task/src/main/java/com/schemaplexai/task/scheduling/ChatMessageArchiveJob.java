package com.schemaplexai.task.scheduling;

import com.schemaplexai.task.service.ChatMessageArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageArchiveJob {

    private final ChatMessageArchiveService archiveService;

    @Value("${task.chat-message-archive.retention-days:90}")
    private int retentionDays = 90;

    @Scheduled(cron = "0 30 1 * * ?")
    @SchedulerLock(name = "chatMessageArchiveJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[ChatMessageArchiveJob] Start chat message archive");
        try {
            int archived = archiveService.archiveExpiredMessages(Duration.ofDays(retentionDays));
            log.info("[ChatMessageArchiveJob] Chat message archive completed, archived={}", archived);
        } catch (Exception e) {
            log.error("[ChatMessageArchiveJob] Chat message archive failed", e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Chat message archive job failed", e);
        }
    }
}
