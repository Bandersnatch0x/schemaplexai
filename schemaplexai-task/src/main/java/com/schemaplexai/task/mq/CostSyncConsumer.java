package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.CostSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Consumes the {@code sf.cost.queue} bound to {@code sf.exchange} / {@code sf.cost}.
 *
 * <p>Dual-mode consumer:
 * <ul>
 *   <li><b>Cost-recorded events</b> — {@link CostRecordedEvent} payloads published by
 *       the Agent engine after every LLM call (message header
 *       {@code eventType=CostRecordedEvent}). The record is persisted to PG
 *       {@code sf_cost_record} via {@link CostService#processCostRecordedEvent},
 *       which also prices the usage and accumulates budget consumption.</li>
 *   <li><b>Sync triggers</b> — legacy {@link CostSyncMessage} payloads requesting an
 *       incremental PostgreSQL → ClickHouse cost sync. Reads the payload, checks
 *       idempotency via Redis, delegates to {@link CostDataSyncService}, and logs
 *       sync start/completion.</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnBean({CostDataSyncService.class, InboxDeduplicationService.class})
@RequiredArgsConstructor
public class CostSyncConsumer {

    private static final String IDEMPOTENCY_PREFIX = "sf:idempotency:cost:sync:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Message header carrying the payload type (producer contract costRecordedEvent). */
    static final String EVENT_TYPE_HEADER = "eventType";
    /** Header value marking a CostRecordedEvent payload. */
    static final String EVENT_TYPE_COST_RECORDED = "CostRecordedEvent";

    private final CostDataSyncService costDataSyncService;
    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final InboxDeduplicationService dedupService;

    /**
     * Cost persistence service. Optional: deployments that only run the sync-trigger
     * half of this consumer may not wire the ops cost beans.
     */
    @Autowired(required = false)
    private CostService costService;

    @RabbitListener(queues = "sf.cost.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String syncId = null;
        UUID eventId = null;

        try {
            log.info("[CostSyncConsumer] Received cost sync message: {}", body);

            if (isCostRecordedEvent(message, body)) {
                handleCostRecordedEvent(body, channel, deliveryTag);
                return;
            }

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

    /**
     * Determines whether the message carries a CostRecordedEvent payload
     * (engine cost collection) rather than a legacy sync trigger.
     *
     * <p>The producer contract stamps the header {@code eventType=CostRecordedEvent};
     * when the header is absent the payload shape is sniffed conservatively —
     * {@link CostSyncMessage} never carries token/model fields.
     */
    private boolean isCostRecordedEvent(Message message, String body) {
        Object eventTypeHeader = message.getMessageProperties().getHeader(EVENT_TYPE_HEADER);
        if (EVENT_TYPE_COST_RECORDED.equals(eventTypeHeader)) {
            return true;
        }
        if (eventTypeHeader != null) {
            return false;
        }
        try {
            JsonNode tree = objectMapper.readTree(body);
            return tree != null && tree.isObject()
                    && (tree.has("inputTokens") || tree.has("modelName") || tree.has("eventId"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Persists an engine-published cost-recorded event to PG {@code sf_cost_record}
     * via {@link CostService#processCostRecordedEvent} (idempotent by eventId).
     * Exceptions propagate to the shared catch blocks (fail log + nack, no requeue).
     */
    private void handleCostRecordedEvent(String body, Channel channel, long deliveryTag) throws IOException {
        CostRecordedEvent event = objectMapper.readValue(body, CostRecordedEvent.class);

        if (event.executionId() == null || event.tenantId() == null) {
            log.warn("[CostSyncConsumer] Cost event missing executionId or tenantId, skipping: {}", body);
            channel.basicAck(deliveryTag, false);
            return;
        }

        UUID eventId = event.eventId() != null
                ? event.eventId()
                : UUID.nameUUIDFromBytes(body.getBytes(StandardCharsets.UTF_8));

        if (dedupService.isProcessed(eventId, "CostSyncConsumer")) {
            log.warn("[CostSyncConsumer] Duplicate cost event detected for eventId={}, skipping", eventId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (costService == null) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "CostService is not available in this deployment; cannot persist cost record");
        }

        log.info("[CostSyncConsumer] Persisting cost record | eventId={} | executionId={} | tenantId={} | model={} | in={} | out={}",
                eventId, event.executionId(), event.tenantId(), event.modelName(),
                event.inputTokens(), event.outputTokens());

        costService.processCostRecordedEvent(event);

        dedupService.markProcessed(eventId, "CostSyncConsumer");
        channel.basicAck(deliveryTag, false);
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
