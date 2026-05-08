package com.schemaplexai.task.config;

import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void schemaplexaiExchange_isDurable() {
        DirectExchange exchange = config.schemaplexaiExchange();
        assertThat(exchange.getName()).isEqualTo(CommonConstants.EXCHANGE_SCHEMAPLEXAI);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void agentExecuteQueue_hasDeadLetterArgs() {
        Queue queue = config.agentExecuteQueue();
        assertThat(queue.getName()).isEqualTo("sf.agent.execute.queue");
        assertThat(queue.isDurable()).isTrue();
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        assertThat(args).containsEntry("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
    }

    @Test
    void agentExecuteBinding_routingKeyMatches() {
        Binding binding = config.agentExecuteBinding();
        assertThat(binding.getExchange()).isEqualTo(CommonConstants.EXCHANGE_SCHEMAPLEXAI);
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_AGENT_EXECUTE);
        assertThat(binding.getDestination()).isEqualTo("sf.agent.execute.queue");
    }

    @Test
    void workflowTriggerQueue_hasDeadLetterArgs() {
        Queue queue = config.workflowTriggerQueue();
        assertThat(queue.getName()).isEqualTo("sf.workflow.trigger.queue");
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
    }

    @Test
    void workflowTriggerBinding_routingKeyMatches() {
        Binding binding = config.workflowTriggerBinding();
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_WORKFLOW_TRIGGER);
    }

    @Test
    void notificationQueue_hasDeadLetterArgs() {
        Queue queue = config.notificationQueue();
        assertThat(queue.getName()).isEqualTo("sf.notification.queue");
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
    }

    @Test
    void notificationBinding_routingKeyMatches() {
        Binding binding = config.notificationBinding();
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_NOTIFICATION);
    }

    @Test
    void costQueue_hasDeadLetterArgs() {
        Queue queue = config.costQueue();
        assertThat(queue.getName()).isEqualTo("sf.cost.queue");
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
    }

    @Test
    void costBinding_routingKeyMatches() {
        Binding binding = config.costBinding();
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_COST);
    }

    @Test
    void qualityQueue_hasDeadLetterArgs() {
        Queue queue = config.qualityQueue();
        assertThat(queue.getName()).isEqualTo("sf.quality.queue");
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
    }

    @Test
    void qualityBinding_routingKeyMatches() {
        Binding binding = config.qualityBinding();
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_QUALITY);
    }

    @Test
    void milvusSyncQueue_hasDeadLetterArgs() {
        Queue queue = config.milvusSyncQueue();
        assertThat(queue.getName()).isEqualTo("sf.milvus.sync.queue");
        Map<String, Object> args = queue.getArguments();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
    }

    @Test
    void milvusSyncBinding_routingKeyMatches() {
        Binding binding = config.milvusSyncBinding();
        assertThat(binding.getRoutingKey()).isEqualTo(CommonConstants.RK_MILVUS_SYNC);
    }
}
