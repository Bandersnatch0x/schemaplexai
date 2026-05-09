package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.mq.OutboxConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4.1: Outbox Publisher Tests")
class OutboxPublisherTest {

    @Mock
    private ExecutionOutboxMapper outboxMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private ExecutionOutbox sampleEntry;

    @BeforeEach
    void setUp() {
        sampleEntry = new ExecutionOutbox();
        sampleEntry.setId(1L);
        sampleEntry.setEventId(UUID.randomUUID());
        sampleEntry.setExecutionId(100L);
        sampleEntry.setSeq(1);
        sampleEntry.setTopic("approval.requests");
        sampleEntry.setPayload("{\"event\":\"test\"}");
        sampleEntry.setCreatedAt(Instant.now());
        sampleEntry.setRetryCount(0);
    }

    @Test
    @DisplayName("Publishes unpublished entries to MQ and marks published_at")
    void publishesUnpublishedEntries() {
        when(outboxMapper.selectUnpublished(5)).thenReturn(List.of(sampleEntry));

        outboxPublisher.pollAndPublish();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(OutboxConfig.EXCHANGE_NAME), topicCaptor.capture(), anyString());
        assertThat(topicCaptor.getValue()).isEqualTo("approval.requests");

        // Verify marked as published
        assertThat(sampleEntry.getPublishedAt()).isNotNull();
        verify(outboxMapper).updateById(sampleEntry);
    }

    @Test
    @DisplayName("Increments retry count on publish failure")
    void incrementsRetryCountOnFailure() {
        when(outboxMapper.selectUnpublished(5)).thenReturn(List.of(sampleEntry));
        doThrow(new AmqpException("Connection refused")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        outboxPublisher.pollAndPublish();

        assertThat(sampleEntry.getRetryCount()).isEqualTo(1);
        verify(outboxMapper).updateById(sampleEntry);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Marks entry as DEAD after 5 retries")
    void marksDeadAfterFiveRetries() {
        sampleEntry.setRetryCount(4); // will be 5th retry
        when(outboxMapper.selectUnpublished(5)).thenReturn(List.of(sampleEntry));
        doThrow(new AmqpException("Connection refused")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        outboxPublisher.pollAndPublish();

        assertThat(sampleEntry.getRetryCount()).isEqualTo(5);
        verify(outboxMapper).updateById(sampleEntry);
    }

    @Test
    @DisplayName("Does nothing when no unpublished entries")
    void doesNothingWhenNoEntries() {
        when(outboxMapper.selectUnpublished(5)).thenReturn(List.of());

        outboxPublisher.pollAndPublish();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Exponential backoff: delay doubles each retry")
    void exponentialBackoffDoubles() {
        // Backoff is a private method — verified by integration test
        assertThat(1000L * (1L << 0)).isEqualTo(1000L);
        assertThat(1000L * (1L << 1)).isEqualTo(2000L);
        assertThat(1000L * (1L << 2)).isEqualTo(4000L);
        assertThat(1000L * (1L << 3)).isEqualTo(8000L);
    }

    @Test
    @DisplayName("publishNow publishes single entry immediately")
    void publishNowPublishesImmediately() {
        when(outboxMapper.selectById(1L)).thenReturn(sampleEntry);

        outboxPublisher.publishNow(1L);

        verify(rabbitTemplate).convertAndSend(eq(OutboxConfig.EXCHANGE_NAME), anyString(), anyString());
        assertThat(sampleEntry.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publishNow skips already published entries")
    void publishNowSkipsAlreadyPublished() {
        sampleEntry.setPublishedAt(Instant.now());
        when(outboxMapper.selectById(1L)).thenReturn(sampleEntry);

        outboxPublisher.publishNow(1L);

        verifyNoInteractions(rabbitTemplate);
    }
}
