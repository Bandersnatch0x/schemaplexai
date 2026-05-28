package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.dto.AgentExecuteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Dispatches agent execution requests received from MQ to the agent-engine service.
 * <p>
 * Reads {@link AgentExecuteMessage} payloads from the {@code sf.agent.execute.queue},
 * checks idempotency via Redis, delegates to {@link AgentExecutionEngine#startExecution},
 * and acknowledges the MQ message on success.
 */
@Slf4j
@Component
@ConditionalOnBean({AgentExecutionEngine.class, InboxDeduplicationService.class})
@RequiredArgsConstructor
public class AgentExecuteDispatcher {

    private final AgentExecutionEngine executionEngine;
    private final MessageFailLogService messageFailLogService;
    private final ObjectMapper objectMapper;
    private final InboxDeduplicationService dedupService;

    @RabbitListener(queues = "sf.agent.execute.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID eventId = null;

        try {
            log.info("[AgentExecuteDispatcher] Received agent execute message: {}", body);

            AgentExecuteMessage payload = objectMapper.readValue(body, AgentExecuteMessage.class);

            if (payload.getAgentId() == null) {
                log.error("[AgentExecuteDispatcher] Missing agentId in message: {}", body);
                throw new BaseException(ResultCode.PARAM_ERROR, "agentId is required");
            }

            eventId = resolveEventId(payload);
            if (dedupService.isProcessed(eventId, "AgentExecuteDispatcher")) {
                log.warn("[AgentExecuteDispatcher] Duplicate execution detected for eventId={}, skipping", eventId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            SfAgentExecution execution = executionEngine.startExecution(
                    payload.getAgentId(),
                    payload.getTenantId(),
                    payload.getPrompt()
            );

            dedupService.markProcessed(eventId, "AgentExecuteDispatcher");

            log.info("[AgentExecuteDispatcher] Dispatched execution {} for agent {}, conversationId: {}",
                    execution.getId(), payload.getAgentId(), execution.getConversationId());

            channel.basicAck(deliveryTag, false);

        } catch (BaseException e) {
            log.error("[AgentExecuteDispatcher] Business error processing message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[AgentExecuteDispatcher] Failed to process message: {}", body, e);
            recordFailLog(message, e.getMessage(), deliveryTag);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void recordFailLog(Message message, String errorMessage, long deliveryTag) {
        boolean persisted = messageFailLogService.log(message, this.getClass().getSimpleName(), errorMessage);
        if (!persisted) {
            log.warn("[AgentExecuteDispatcher] Message fail log persistence returned false for deliveryTag={}",
                    deliveryTag);
        }
    }

    private UUID resolveEventId(AgentExecuteMessage payload) {
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            return UUID.nameUUIDFromBytes(payload.getIdempotencyKey().getBytes(StandardCharsets.UTF_8));
        }
        String base = payload.getAgentId() + ":" + payload.getTenantId() + ":" + payload.getPrompt();
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }
}
