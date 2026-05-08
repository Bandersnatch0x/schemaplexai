package com.schemaplexai.task.mq.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * MQ message post-processor that extracts tenantId from the message body
 * and sets it in {@link TenantContextHolder} before the consumer processes the message.
 * <p>
 * Messages with a missing or blank tenantId are rejected and routed to the dead-letter queue
 * to prevent cross-tenant data leakage.
 */
@Slf4j
public class TenantMqFilter implements MessagePostProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Message postProcessMessage(Message message) {
        try {
            String body = new String(message.getBody());
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode tenantNode = root.get("tenantId");

            if (tenantNode == null || tenantNode.isNull() || tenantNode.asText().isBlank()) {
                String routingKey = message.getMessageProperties().getReceivedRoutingKey();
                log.error("[TenantMqFilter] Rejected message with missing tenantId on routingKey={}", routingKey);
                throw new AmqpRejectAndDontRequeueException("tenantId is required in MQ message body");
            }

            String tenantId = tenantNode.asText();
            TenantContextHolder.setTenantId(tenantId);
            log.debug("[TenantMqFilter] Set tenant context: tenantId={}", tenantId);

        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TenantMqFilter] Failed to extract tenantId from message", e);
            throw new AmqpRejectAndDontRequeueException("Failed to parse tenantId from MQ message", e);
        }

        return message;
    }
}
