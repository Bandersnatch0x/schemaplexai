package com.schemaplexai.agent.engine.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for execution events.
 */
@Configuration
public class OutboxConfig {

    public static final String EXCHANGE_NAME = "execution_events";

    // Outbox retry queue
    public static final String DEAD_LETTER_QUEUE = "execution.dead-letter";

    /**
     * Main exchange for all execution events.
     */
    @Bean
    public TopicExchange executionEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Dead letter queue for permanently failed outbox entries.
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(executionEventsExchange())
                .with("DEAD.#");
    }
}
