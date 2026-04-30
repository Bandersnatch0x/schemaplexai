package com.schemaplexai.task.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryConsolidationJob {

    private final StringRedisTemplate redisTemplate;
    private static final String CHAT_MEMORY_KEY_PREFIX = "chat:memory:";
    private static final Duration MEMORY_TTL = Duration.ofDays(7);

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "memoryConsolidationJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.info("[MemoryConsolidationJob] Start memory consolidation");
        try {
            // Refresh TTL on active chat memories
            Set<String> keys = redisTemplate.keys(CHAT_MEMORY_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    redisTemplate.expire(key, MEMORY_TTL);
                }
                log.info("[MemoryConsolidationJob] Refreshed TTL for {} chat memory keys", keys.size());
            }

            // Phase 2: Summarize long conversations, archive old memories
            log.info("[MemoryConsolidationJob] Memory consolidation completed");
        } catch (Exception e) {
            log.error("[MemoryConsolidationJob] Memory consolidation failed", e);
        }
    }
}
