package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.ClickHouseCostSyncService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Triggers cost data synchronization from PostgreSQL to ClickHouse.
 * <p>
 * Reads {@link CostSyncMessage} payloads from the {@code sf.cost.queue},
 * checks idempotency via Redis, delegates to {@link ClickHouseCostSyncService},
 * and logs sync start/completion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CostSyncConsumer {

    private static final String IDEMPOTENCY_PREFIX = "sf:idempotency:cost:sync:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(1);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClickHouseCostSyncService clickHouseCostSyncService;
    private final MessageFailLogService messageFailLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "sf.cost.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String syncId = null;

        try {
            log.info("[CostSyncConsumer] Received cost sync message: {}", body);

            CostSyncMessage payload = objectMapper.readValue(body, CostSyncMessage.class);
            syncId = resolveSyncId(payload);

            if (isAlreadySynced(syncId)) {
                log.warn("[CostSyncConsumer] Duplicate sync detected for key: {}, skipping", syncId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("[CostSyncConsumer] Starting cost sync | syncId={} | type={} | tenantId={} | dateRange={} | forceFullSync={}",
                    syncId, payload.getSyncType(), payload.getTenantId(), payload.getDateRange(), payload.getForceFullSync());

            clickHouseCostSyncService.syncIncrementalData();

            markAsSynced(syncId);

            log.info("[CostSyncConsumer] Cost sync completed successfully | syncId={} | completedAt={}",
                    syncId, LocalDateTime.now().format(DATE_FMT));

            channel.basicAck(deliveryTag, false);

        } catch (BaseException e) {
            log.error("[CostSyncConsumer] Business error during sync | syncId={} | error={}", syncId, e.getMessage(), e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[CostSyncConsumer] Failed to process cost sync message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String resolveSyncId(CostSyncMessage payload) {
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            return IDEMPOTENCY_PREFIX + payload.getIdempotencyKey();
        }
        String base = payload.getSyncType() != null ? payload.getSyncType() : "incremental";
        if (payload.getTenantId() != null) {
            base += ":" + payload.getTenantId();
        }
        return IDEMPOTENCY_PREFIX + base + ":" + System.currentTimeMillis();
    }

    private boolean isAlreadySynced(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    private void markAsSynced(String key) {
        redisTemplate.opsForValue().set(key, LocalDateTime.now().format(DATE_FMT), IDEMPOTENCY_TTL);
    }
}
