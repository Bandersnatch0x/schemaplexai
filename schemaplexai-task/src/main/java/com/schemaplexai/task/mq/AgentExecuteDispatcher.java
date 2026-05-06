package com.schemaplexai.task.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.AgentExecuteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Dispatches agent execution requests received from MQ to the agent-engine service.
 * <p>
 * Reads {@link AgentExecuteMessage} payloads from the {@code sf.agent.execute.queue},
 * checks idempotency via Redis, delegates to {@link AgentExecutionEngine#startExecution},
 * and acknowledges the MQ message on success.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecuteDispatcher {

    private static final String IDEMPOTENCY_PREFIX = "sf:idempotency:agent:execute:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final AgentExecutionEngine executionEngine;
    private final MessageFailLogService messageFailLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "sf.agent.execute.queue")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            log.info("[AgentExecuteDispatcher] Received agent execute message: {}", body);

            AgentExecuteMessage payload = objectMapper.readValue(body, AgentExecuteMessage.class);

            if (payload.getAgentId() == null) {
                log.error("[AgentExecuteDispatcher] Missing agentId in message: {}", body);
                throw new BaseException(ResultCode.PARAM_ERROR, "agentId is required");
            }

            String idempotencyKey = resolveIdempotencyKey(payload);
            if (isAlreadyExecuted(idempotencyKey)) {
                log.warn("[AgentExecuteDispatcher] Duplicate execution detected for key: {}, skipping", idempotencyKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            SfAgentExecution execution = executionEngine.startExecution(
                    payload.getAgentId(),
                    payload.getTenantId(),
                    payload.getPrompt()
            );

            markAsExecuted(idempotencyKey);

            log.info("[AgentExecuteDispatcher] Dispatched execution {} for agent {}, conversationId: {}",
                    execution.getId(), payload.getAgentId(), execution.getConversationId());

            channel.basicAck(deliveryTag, false);

        } catch (BaseException e) {
            log.error("[AgentExecuteDispatcher] Business error processing message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("[AgentExecuteDispatcher] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String resolveIdempotencyKey(AgentExecuteMessage payload) {
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            return IDEMPOTENCY_PREFIX + payload.getIdempotencyKey();
        }
        return IDEMPOTENCY_PREFIX + payload.getAgentId() + ":" + payload.getTenantId() + ":" + payload.getPrompt();
    }

    private boolean isAlreadyExecuted(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    private void markAsExecuted(String key) {
        redisTemplate.opsForValue().set(key, "1", IDEMPOTENCY_TTL);
    }
}
