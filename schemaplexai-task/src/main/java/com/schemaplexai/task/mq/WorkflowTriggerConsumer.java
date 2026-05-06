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
 * Triggers workflow instances from MQ messages.
 * <p>
 * TODO: Implement the following:
 * <ol>
 *   <li>Parse the MQ message payload into a WorkflowTriggerMessage DTO (define fields: workflowDefinitionKey, businessKey, variables, tenantId, triggerSource, idempotencyKey).</li>
 *   <li>Check idempotency via Redis to avoid duplicate workflow instances for the same businessKey.</li>
 *   <li>Resolve the workflow definition from Flowable repository by workflowDefinitionKey.</li>
 *   <li>Start a new process instance via Flowable RuntimeService with the provided variables.</li>
 *   <li>Log the triggered instance ID, definition key, and business key.</li>
 *   <li>Handle specific errors:
 *       <ul>
 *         <li>Workflow definition not found - log and nack (no retry).</li>
 *         <li>Flowable engine unavailable - nack with requeue for retry.</li>
 *       </ul>
 *   </li>
 *   <li>On unhandled failure, nack the message so it routes to the dead-letter queue.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTriggerConsumer {

    private final MessageFailLogService messageFailLogService;

    @RabbitListener(queues = "sf.workflow.trigger.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[WorkflowTriggerConsumer] Received workflow trigger message: {}", body);
            // TODO: Parse payload, check idempotency, resolve workflow definition, start Flowable instance, log result
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[WorkflowTriggerConsumer] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
