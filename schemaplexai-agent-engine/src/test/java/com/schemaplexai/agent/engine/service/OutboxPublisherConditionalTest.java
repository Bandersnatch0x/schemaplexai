package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxPublisherConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ExecutionOutboxMapper.class, () -> mock(ExecutionOutboxMapper.class))
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withUserConfiguration(OutboxPublisher.class);

    @Test
    void createsOutboxPublisherByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(OutboxPublisher.class)).hasSize(1));
    }

    @Test
    void skipsOutboxPublisherWhenDisabled() {
        contextRunner
                .withPropertyValues("outbox.publisher.enabled=false")
                .run(context ->
                        assertThat(context.getBeansOfType(OutboxPublisher.class)).isEmpty());
    }
}
