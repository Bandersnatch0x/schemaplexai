package com.schemaplexai.task.scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckJobTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private HealthCheckJob job;

    @Test
    void run_scheduleDelaysFirstCheckUntilApplicationHasStarted() throws Exception {
        Method run = HealthCheckJob.class.getDeclaredMethod("run");
        Scheduled scheduled = run.getAnnotation(Scheduled.class);

        assertThat(scheduled.initialDelayString())
                .isEqualTo("${schemaplexai.task.health-check.initial-delay:60000}");
    }

    @Test
    void run_dependenciesOk_checksRedisDbAndRabbitMq() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenReturn("ok");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(rabbitTemplate.execute(any(ChannelCallback.class))).thenReturn(Boolean.TRUE);

        job.run();

        verify(valueOperations).get("health:check");
        verify(dataSource).getConnection();
        verify(connection).isValid(2);
        verify(connection).close();
        verify(rabbitTemplate).execute(any(ChannelCallback.class));
    }

    @Test
    void run_redisThrowsException_doesNotThrow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenThrow(new RuntimeException("Redis down"));

        assertThatNoException().isThrownBy(() -> job.run());
    }

    @Test
    void run_dbThrowsException_doesNotThrowAndStillChecksRabbitMq() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenReturn("ok");
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB down"));
        when(rabbitTemplate.execute(any(ChannelCallback.class))).thenReturn(Boolean.TRUE);

        assertThatNoException().isThrownBy(() -> job.run());

        verify(dataSource).getConnection();
        verify(rabbitTemplate).execute(any(ChannelCallback.class));
    }

    @Test
    void run_rabbitMqThrowsException_doesNotThrow() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenReturn("ok");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        when(rabbitTemplate.execute(any(ChannelCallback.class))).thenThrow(new RuntimeException("MQ down"));

        assertThatNoException().isThrownBy(() -> job.run());
    }
}
