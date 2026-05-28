package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.WorkflowTriggerMessage;
import com.schemaplexai.task.service.WorkflowTriggerRequestHandler;
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
    private final ObjectMapper objectMapper;
    private final WorkflowTriggerRequestHandler triggerRequestHandler;

    @RabbitListener(queues = "sf.workflow.trigger.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            log.info("[WorkflowTriggerConsumer] Received workflow trigger message: {}", body);
            WorkflowTriggerMessage payload = objectMapper.readValue(body, WorkflowTriggerMessage.class);
            validatePayload(payload);
            triggerRequestHandler.handle(payload);
            channel.basicAck(deliveryTag, false);
        } catch (BaseException e) {
            log.error("[WorkflowTriggerConsumer] Business error processing message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[WorkflowTriggerConsumer] Failed to process message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validatePayload(WorkflowTriggerMessage payload) {
        if (payload == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "workflow trigger payload must be a JSON object");
        }
        if (payload.getWorkflowDefinitionKey() == null || payload.getWorkflowDefinitionKey().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "workflowDefinitionKey is required");
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[WorkflowTriggerConsumer] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }
}
