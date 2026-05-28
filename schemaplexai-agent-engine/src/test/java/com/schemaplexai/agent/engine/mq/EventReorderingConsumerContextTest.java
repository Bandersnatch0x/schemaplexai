package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.service.ExecutionEventBuffer;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventReorderingConsumerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ExecutionEventBuffer.class, () -> mock(ExecutionEventBuffer.class))
            .withBean(ExecutionEventService.class, () -> mock(ExecutionEventService.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(EventReorderingConsumer.class);

    @Test
    void startsWhenGapRecoveryJobIsDisabled() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(EventReorderingConsumer.class)).hasSize(1));
    }
}
