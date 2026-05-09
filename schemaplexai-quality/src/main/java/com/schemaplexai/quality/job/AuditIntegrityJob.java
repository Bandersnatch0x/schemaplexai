package com.schemaplexai.quality.job;

import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import com.schemaplexai.quality.mq.AuditEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Periodic background job that validates audit event integrity.
 * Re-computes content_hash for events in the last 24h and flags mismatches.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditIntegrityJob {

    private final AuditEventMapper auditEventMapper;

    @Scheduled(cron = "0 30 2 * * *")
    @SchedulerLock(name = "auditIntegrityCheck", lockAtMostFor = "PT30M")
    public void runIntegrityCheck() {
        log.info("[AuditIntegrityJob] Starting integrity check for last 24h");

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<SfAuditEvent> events = auditEventMapper.selectRecentEvents(since);

        int checked = 0;
        int corrupted = 0;

        for (SfAuditEvent event : events) {
            if (event.getContentHash() == null) {
                checked++;
                continue; // Skip events without hash (pre-projection or legacy)
            }
            String expected = recomputeHash(event);
            if (!expected.equals(event.getContentHash())) {
                corrupted++;
                log.error("[AuditIntegrityJob] CORRUPTED event id={} execution={} expected={} actual={}",
                        event.getId(), event.getExecutionId(), expected, event.getContentHash());
                auditEventMapper.markCorrupted(event.getId());
            }
            checked++;
        }

        log.info("[AuditIntegrityJob] Completed: checked={}, corrupted={}", checked, corrupted);
    }

    private String recomputeHash(SfAuditEvent event) {
        try {
            String raw = event.getEventType() + "|" + event.getDetailsJson() + "|" + event.getOccurredAt();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
