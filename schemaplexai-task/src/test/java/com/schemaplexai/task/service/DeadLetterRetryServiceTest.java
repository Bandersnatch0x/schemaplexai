package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.6: Dead Letter Retry Service Tests")
class DeadLetterRetryServiceTest {

    @Mock
    private SfMessageFailLogMapper messageFailLogMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DeadLetterRetryService deadLetterRetryService;

    @Test
    @DisplayName("retryDeadEvent republishes pending failed message and marks it retried")
    void retryDeadEvent_republishesPendingMessageAndMarksRetried() {
        UUID eventId = UUID.randomUUID();
        SfMessageFailLog failLog = pendingFailLog(eventId, "sf.exchange", "sf.notification", "{\"id\":1}", 2);

        when(messageFailLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(failLog);
        when(messageFailLogMapper.updateById(any(SfMessageFailLog.class))).thenReturn(1);

        deadLetterRetryService.retryDeadEvent(eventId);

        verify(rabbitTemplate).convertAndSend("sf.exchange", "sf.notification", "{\"id\":1}");

        ArgumentCaptor<SfMessageFailLog> captor = ArgumentCaptor.forClass(SfMessageFailLog.class);
        verify(messageFailLogMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("RETRIED");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("retryDeadEvent fails explicitly when no pending failed message exists")
    void retryDeadEvent_missingPendingMessageFailsExplicitly() {
        UUID eventId = UUID.randomUUID();

        when(messageFailLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> deadLetterRetryService.retryDeadEvent(eventId))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("not found")
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(String.class));
    }

    @Test
    @DisplayName("listDeadEvents returns recent pending failed messages")
    void listDeadEvents_returnsRecentPendingFailedMessages() {
        SfMessageFailLog first = pendingFailLog(UUID.randomUUID(), "sf.exchange", "sf.quality", "{}", 0);
        SfMessageFailLog second = pendingFailLog(UUID.randomUUID(), "sf.exchange", "sf.cost", "{}", 1);
        when(messageFailLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));

        List<SfMessageFailLog> result = deadLetterRetryService.listDeadEvents(10);

        assertThat(result).containsExactly(first, second);
        verify(messageFailLogMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listDeadEvents rejects non-positive limits")
    void listDeadEvents_rejectsNonPositiveLimit() {
        assertThatThrownBy(() -> deadLetterRetryService.listDeadEvents(0))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("limit")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());

        verify(messageFailLogMapper, never()).selectList(any());
    }

    private SfMessageFailLog pendingFailLog(
            UUID messageId,
            String exchange,
            String routingKey,
            String payload,
            int retryCount) {
        SfMessageFailLog failLog = new SfMessageFailLog();
        failLog.setId(1L);
        failLog.setMessageId(messageId.toString());
        failLog.setExchange(exchange);
        failLog.setRoutingKey(routingKey);
        failLog.setPayload(payload);
        failLog.setConsumerGroup("TestConsumer");
        failLog.setErrorMsg("boom");
        failLog.setStatus("PENDING");
        failLog.setRetryCount(retryCount);
        return failLog;
    }
}
