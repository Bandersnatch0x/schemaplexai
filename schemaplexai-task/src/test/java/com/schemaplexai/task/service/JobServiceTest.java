package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import com.schemaplexai.task.vo.JobPageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Job board service (JobService) tests")
class JobServiceTest {

    @Mock
    private SfMessageFailLogMapper messageFailLogMapper;

    @Mock
    private DeadLetterRetryService deadLetterRetryService;

    @InjectMocks
    private JobService jobService;

    @Test
    @DisplayName("listJobs projects fail logs into the {list,total} JobRecord envelope")
    void listJobs_projectsFailLogsIntoJobRecordEnvelope() {
        SfMessageFailLog failLog = failLog(21L, "msg-uuid-1", "sf.cost", "cost-consumer", "PENDING", 2);
        Page<SfMessageFailLog> page = new Page<>(1, 10);
        page.setRecords(List.of(failLog));
        page.setTotal(1);
        when(messageFailLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        JobPageResult result = jobService.listJobs(1, 10, "sf.cost");

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        var job = result.getList().get(0);
        assertThat(job.getId()).isEqualTo("21");
        assertThat(job.getName()).isEqualTo("msg-uuid-1");
        assertThat(job.getQueue()).isEqualTo("sf.cost");
        assertThat(job.getStatus()).isEqualTo("PENDING");
        assertThat(job.getRetryCount()).isEqualTo(2);
        assertThat(job.getMaxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("listJobs falls back to row id name and consumer group queue")
    void listJobs_fallsBackForMissingMessageIdAndRoutingKey() {
        SfMessageFailLog failLog = failLog(22L, null, null, "quality-consumer", "RETRIED", null);
        Page<SfMessageFailLog> page = new Page<>(1, 10);
        page.setRecords(List.of(failLog));
        page.setTotal(1);
        when(messageFailLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        JobPageResult result = jobService.listJobs(0, 0, null);

        var job = result.getList().get(0);
        assertThat(job.getName()).isEqualTo("fail-log-22");
        assertThat(job.getQueue()).isEqualTo("quality-consumer");
        assertThat(job.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("retryJob delegates to the dead letter replay for UUID message ids")
    void retryJob_delegatesToDeadLetterReplayForUuidMessageIds() {
        UUID eventId = UUID.randomUUID();
        SfMessageFailLog failLog = failLog(5L, eventId.toString(), "sf.exchange", "g", "PENDING", 0);
        when(messageFailLogMapper.selectById(5L)).thenReturn(failLog);

        jobService.retryJob(5L);

        verify(deadLetterRetryService).retryDeadEvent(eq(eventId));
        verify(messageFailLogMapper, never()).updateById(any(SfMessageFailLog.class));
    }

    @Test
    @DisplayName("retryJob marks records without a UUID message id as RETRY_PENDING")
    void retryJob_marksNonUuidRecordsRetryPending() {
        SfMessageFailLog failLog = failLog(6L, "not-a-uuid", "sf.exchange", "g", "PENDING", 1);
        when(messageFailLogMapper.selectById(6L)).thenReturn(failLog);
        when(messageFailLogMapper.updateById(any(SfMessageFailLog.class))).thenReturn(1);

        jobService.retryJob(6L);

        verifyNoInteractions(deadLetterRetryService);
        ArgumentCaptor<SfMessageFailLog> captor = ArgumentCaptor.forClass(SfMessageFailLog.class);
        verify(messageFailLogMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("RETRY_PENDING");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(2);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("retryJob only accepts PENDING jobs")
    void retryJob_onlyAcceptsPendingJobs() {
        SfMessageFailLog failLog = failLog(7L, UUID.randomUUID().toString(), "sf.exchange", "g", "CANCELLED", 0);
        when(messageFailLogMapper.selectById(7L)).thenReturn(failLog);

        assertThatThrownBy(() -> jobService.retryJob(7L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("not retryable")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
        verifyNoInteractions(deadLetterRetryService);
    }

    @Test
    @DisplayName("retryJob fails with NOT_FOUND for a missing job")
    void retryJob_missingJobFailsWithNotFound() {
        when(messageFailLogMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> jobService.retryJob(404L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("cancelJob marks pending jobs CANCELLED")
    void cancelJob_marksPendingJobsCancelled() {
        SfMessageFailLog failLog = failLog(8L, "m", "sf.exchange", "g", "PENDING", 0);
        when(messageFailLogMapper.selectById(8L)).thenReturn(failLog);
        when(messageFailLogMapper.updateById(any(SfMessageFailLog.class))).thenReturn(1);

        jobService.cancelJob(8L);

        ArgumentCaptor<SfMessageFailLog> captor = ArgumentCaptor.forClass(SfMessageFailLog.class);
        verify(messageFailLogMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelJob is idempotent for already cancelled jobs")
    void cancelJob_idempotentForCancelledJobs() {
        SfMessageFailLog failLog = failLog(9L, "m", "sf.exchange", "g", "CANCELLED", 0);
        when(messageFailLogMapper.selectById(9L)).thenReturn(failLog);

        jobService.cancelJob(9L);

        verify(messageFailLogMapper, never()).updateById(any(SfMessageFailLog.class));
    }

    @Test
    @DisplayName("cancelJob rejects jobs that were already republished")
    void cancelJob_rejectsRepublishedJobs() {
        SfMessageFailLog failLog = failLog(10L, "m", "sf.exchange", "g", "RETRIED", 1);
        when(messageFailLogMapper.selectById(10L)).thenReturn(failLog);

        assertThatThrownBy(() -> jobService.cancelJob(10L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("already republished")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("cancelJob fails with NOT_FOUND for a missing job")
    void cancelJob_missingJobFailsWithNotFound() {
        when(messageFailLogMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> jobService.cancelJob(404L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
    }

    private SfMessageFailLog failLog(Long id, String messageId, String routingKey, String consumerGroup,
                                     String status, Integer retryCount) {
        SfMessageFailLog failLog = new SfMessageFailLog();
        failLog.setId(id);
        failLog.setMessageId(messageId);
        failLog.setExchange("sf.exchange");
        failLog.setRoutingKey(routingKey);
        failLog.setConsumerGroup(consumerGroup);
        failLog.setStatus(status);
        failLog.setRetryCount(retryCount);
        failLog.setCreatedAt(LocalDateTime.of(2026, 8, 29, 12, 0));
        failLog.setUpdatedAt(LocalDateTime.of(2026, 8, 29, 12, 5));
        return failLog;
    }
}
