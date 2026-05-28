package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Triggers cost data synchronization from PostgreSQL to ClickHouse.
 * <p>
 * Reads {@link CostSyncMessage} payloads from the {@code sf.cost.queue},
 * checks idempotency via Redis, delegates to {@link CostDataSyncService},
 * and logs sync start/completion.
 */
@Slf4j
@Component
@ConditionalOnBean({CostDataSyncService.class, InboxDeduplicationService.class})
@RequiredArgsConstructor
public class CostSyncConsumer {

    private static final String IDEMPOTENCY_PREFIX = "sf:idempotency:cost:sync:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CostDataSyncService costDataSyncService;
    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final InboxDeduplicationService dedupService;

    @RabbitListener(queues = "sf.cost.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String syncId = null;
        UUID eventId = null;

        try {
            log.info("[CostSyncConsumer] Received cost sync message: {}", body);

            CostSyncMessage payload = objectMapper.readValue(body, CostSyncMessage.class);
            syncId = resolveSyncId(payload);
            eventId = resolveEventId(payload, syncId);

            if (dedupService.isProcessed(eventId, "CostSyncConsumer")) {
                log.warn("[CostSyncConsumer] Duplicate sync detected for eventId={} | key={}, skipping", eventId, syncId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("[CostSyncConsumer] Starting cost sync | syncId={} | type={} | tenantId={} | dateRange={} | forceFullSync={}",
                    syncId, payload.getSyncType(), payload.getTenantId(), payload.getDateRange(), payload.getForceFullSync());

            validateIncrementalOnlyRequest(payload);
            costDataSyncService.syncIncrementalData();

            dedupService.markProcessed(eventId, "CostSyncConsumer");

            log.info("[CostSyncConsumer] Cost sync completed successfully | syncId={} | completedAt={}",
                    syncId, LocalDateTime.now().format(DATE_FMT));

            channel.basicAck(deliveryTag, false);

        } catch (BaseException e) {
            log.error("[CostSyncConsumer] Business error during sync | syncId={} | error={}", syncId, e.getMessage(), e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[CostSyncConsumer] Failed to process cost sync message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[CostSyncConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
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

    private UUID resolveEventId(CostSyncMessage payload, String syncId) {
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            return UUID.nameUUIDFromBytes(payload.getIdempotencyKey().getBytes(StandardCharsets.UTF_8));
        }
        return UUID.nameUUIDFromBytes(syncId.getBytes(StandardCharsets.UTF_8));
    }

    private void validateIncrementalOnlyRequest(CostSyncMessage payload) {
        if (Boolean.TRUE.equals(payload.getForceFullSync())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Full cost sync is not supported by CostSyncConsumer");
        }
        if (payload.getDateRange() != null && !payload.getDateRange().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Date-range cost sync is not supported by CostSyncConsumer");
        }
        String syncType = payload.getSyncType();
        if (syncType == null || syncType.isBlank()) {
            return;
        }
        String normalizedSyncType = syncType.trim().toLowerCase(Locale.ROOT);
        if (!"incremental".equals(normalizedSyncType) && !"api".equals(normalizedSyncType)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Unsupported cost sync type: " + syncType);
        }
    }
}
