package com.schemaplexai.task.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Processes Milvus vector database synchronization requests from MQ.
 * <p>
 * TODO: Implement the following:
 * <ol>
 *   <li>Parse the MQ message payload into a MilvusSyncMessage DTO (define fields: collectionName, operation, documents[], tenantId, idempotencyKey).</li>
 *   <li>Check idempotency via Redis to avoid duplicate sync operations.</li>
 *   <li>Route by operation type:
 *       <ul>
 *         <li>{@code UPSERT} - Insert or update vector embeddings in the specified Milvus collection.</li>
 *         <li>{@code DELETE} - Remove vectors by primary key from the specified collection.</li>
 *         <li>{@code REBUILD} - Drop and recreate collection index, then re-ingest all documents.</li>
 *       </ul>
 *   </li>
 *   <li>Delegate to a MilvusVectorService (to be created in context module) for actual vector operations.</li>
 *   <li>Log sync metrics (document count, latency, success/failure).</li>
 *   <li>On failure, nack the message so it routes to the dead-letter queue for retry.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusSyncConsumer {

    private final MessageFailLogService messageFailLogService;

    @RabbitListener(queues = "sf.milvus.sync.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[MilvusSyncConsumer] Received milvus sync message: {}", body);
            // TODO: Parse payload, check idempotency, delegate to MilvusVectorService, log metrics
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MilvusSyncConsumer] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
