package com.schemaplexai.agent.engine.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GapRecoveryJobConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ExecutionEventMapper.class, () -> mock(ExecutionEventMapper.class))
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(GapRecoveryJob.class);

    @Test
    void createsGapRecoveryJobByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(GapRecoveryJob.class)).hasSize(1));
    }

    @Test
    void skipsGapRecoveryJobWhenDisabled() {
        contextRunner
                .withPropertyValues("agent.execution.gap-recovery.enabled=false")
                .run(context ->
                        assertThat(context.getBeansOfType(GapRecoveryJob.class)).isEmpty());
    }
}
