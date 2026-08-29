package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.MilvusSyncMessage;
import com.schemaplexai.task.service.MilvusSyncRequestHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Processes Milvus vector database synchronization requests from {@code sf.milvus.sync.queue}
 * (spec §3.4 consumer). Manual ACK is configured globally for this module's listeners.
 * <p>
 * Flow:
 * <ol>
 *   <li>Parse the payload into {@link MilvusSyncMessage} and validate it
 *       (operation must be {@code SYNC_DOC}, docId present).</li>
 *   <li>Delegate to {@link MilvusSyncRequestHandler}, whose production implementation runs
 *       the seven-step sync by calling the context service over HTTP.</li>
 *   <li>On success, ack the delivery. On any failure, record the message in
 *       {@code sf_message_fail_log} and nack without requeue so it routes to the
 *       dead-letter exchange for manual retry; the daily reconciliation re-dispatches
 *       docs still stuck in PENDING/FAILED.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusSyncConsumer {

    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final MilvusSyncRequestHandler syncRequestHandler;

    @RabbitListener(queues = "sf.milvus.sync.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[MilvusSyncConsumer] Received milvus sync message: {}", body);
            MilvusSyncMessage payload = objectMapper.readValue(body, MilvusSyncMessage.class);
            validatePayload(payload);
            syncRequestHandler.handle(payload);
            channel.basicAck(deliveryTag, false);
        } catch (BaseException e) {
            log.error("[MilvusSyncConsumer] Business error processing message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[MilvusSyncConsumer] Failed to process message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validatePayload(MilvusSyncMessage payload) {
        if (payload == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "milvus sync payload must be a JSON object");
        }
        if (payload.getCollectionName() == null || payload.getCollectionName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "collectionName is required");
        }
        String operation = payload.getOperation();
        if (operation == null || operation.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "operation is required");
        }
        if (!"SYNC_DOC".equals(operation)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "unsupported milvus sync operation: " + operation);
        }
        if (payload.getDocId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required for SYNC_DOC");
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[MilvusSyncConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }
}
