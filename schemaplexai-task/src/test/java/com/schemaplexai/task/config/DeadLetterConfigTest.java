package com.schemaplexai.task.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeadLetterConfigTest {

    private final DeadLetterConfig config = new DeadLetterConfig();

    @Test
    void deadLetterExchange_isDurableAndNotAutoDelete() {
        DirectExchange exchange = config.deadLetterExchange();
        assertThat(exchange.getName()).isEqualTo(DeadLetterConfig.DLX_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void deadLetterQueue_isDurable() {
        Queue queue = config.deadLetterQueue();
        assertThat(queue.getName()).isEqualTo(DeadLetterConfig.DLX_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void deadLetterBinding_routingKeyMatches() {
        DirectExchange exchange = config.deadLetterExchange();
        Queue queue = config.deadLetterQueue();
        Binding binding = config.deadLetterBinding();

        assertThat(binding.getExchange()).isEqualTo(exchange.getName());
        assertThat(binding.getRoutingKey()).isEqualTo(DeadLetterConfig.DLX_ROUTING_KEY);
        assertThat(binding.getDestination()).isEqualTo(queue.getName());
    }

    @Test
    void getDeadLetterArgs_containsRequiredKeys() {
        Map<String, Object> args = DeadLetterConfig.getDeadLetterArgs();
        assertThat(args).containsEntry("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        assertThat(args).containsEntry("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
        assertThat(args).containsEntry("x-message-ttl", 60000);
    }
}
