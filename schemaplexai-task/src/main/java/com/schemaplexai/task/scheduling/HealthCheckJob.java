package com.schemaplexai.task.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckJob {

    private final StringRedisTemplate redisTemplate;
    private final DataSource dataSource;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(
            fixedRateString = "${schemaplexai.task.health-check.fixed-rate:60000}",
            initialDelayString = "${schemaplexai.task.health-check.initial-delay:60000}"
    )
    @SchedulerLock(name = "healthCheckJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void run() {
        log.debug("[HealthCheckJob] Start health check");
        try {
            checkRedis();
            checkDatabase();
            checkRabbitMq();
            log.debug("[HealthCheckJob] Health check completed");
        } catch (Exception e) {
            log.error("[HealthCheckJob] Health check failed", e);
        }
    }

    private void checkRedis() {
        try {
            redisTemplate.opsForValue().get("health:check");
            log.debug("[HealthCheckJob] Redis connectivity: OK");
        } catch (Exception e) {
            log.error("[HealthCheckJob] Redis connectivity: FAILED", e);
        }
    }

    private void checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            if (valid) {
                log.debug("[HealthCheckJob] Database connectivity: OK");
            } else {
                log.error("[HealthCheckJob] Database connectivity: FAILED");
            }
        } catch (Exception e) {
            log.error("[HealthCheckJob] Database connectivity: FAILED", e);
        }
    }

    private void checkRabbitMq() {
        try {
            Boolean open = rabbitTemplate.execute(channel -> channel != null && channel.isOpen());
            if (Boolean.TRUE.equals(open)) {
                log.debug("[HealthCheckJob] RabbitMQ connectivity: OK");
            } else {
                log.error("[HealthCheckJob] RabbitMQ connectivity: FAILED");
            }
        } catch (Exception e) {
            log.error("[HealthCheckJob] RabbitMQ connectivity: FAILED", e);
        }
    }
}
