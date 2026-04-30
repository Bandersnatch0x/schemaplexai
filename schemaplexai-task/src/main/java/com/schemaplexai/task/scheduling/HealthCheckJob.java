package com.schemaplexai.task.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckJob {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "healthCheckJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.debug("[HealthCheckJob] Start health check");
        try {
            // Check Redis connectivity
            try {
                redisTemplate.opsForValue().get("health:check");
                log.debug("[HealthCheckJob] Redis connectivity: OK");
            } catch (Exception e) {
                log.error("[HealthCheckJob] Redis connectivity: FAILED", e);
            }

            // Phase 2: Add DB, MQ, Milvus health checks
            log.debug("[HealthCheckJob] Health check completed");
        } catch (Exception e) {
            log.error("[HealthCheckJob] Health check failed", e);
        }
    }
}
