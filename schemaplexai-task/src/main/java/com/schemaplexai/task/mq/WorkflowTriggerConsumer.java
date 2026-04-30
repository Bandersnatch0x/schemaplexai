package com.schemaplexai.task.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
            // TODO: trigger workflow instance
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[WorkflowTriggerConsumer] Failed to process message: {}", body, e);
            messageFailLogService.log(message, this.getClass().getSimpleName(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
