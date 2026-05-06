package com.schemaplexai.admin.service;

import com.schemaplexai.admin.dto.AuditLogQuery;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private SfAuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditLogService, "baseMapper", auditLogMapper);
    }

    // ------------------------------------------------------------------
    // getRecentLogsByTenant
    // ------------------------------------------------------------------

    @Test
    void getRecentLogsByTenant_nullTenantId_throwsParamError() {
        assertThatThrownBy(() -> auditLogService.getRecentLogsByTenant(null, 10))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getRecentLogsByTenant_blankTenantId_throwsParamError() {
        assertThatThrownBy(() -> auditLogService.getRecentLogsByTenant("   ", 10))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getRecentLogsByTenant_nullLimit_usesDefault() {
        when(auditLogMapper.selectRecentByTenantId("tenant-1", 50)).thenReturn(Collections.emptyList());

        List<SfAuditLog> result = auditLogService.getRecentLogsByTenant("tenant-1", null);

        assertThat(result).isEmpty();
        verify(auditLogMapper).selectRecentByTenantId("tenant-1", 50);
    }

    @Test
    void getRecentLogsByTenant_negativeLimit_usesDefault() {
        when(auditLogMapper.selectRecentByTenantId("tenant-1", 50)).thenReturn(Collections.emptyList());

        List<SfAuditLog> result = auditLogService.getRecentLogsByTenant("tenant-1", -5);

        assertThat(result).isEmpty();
        verify(auditLogMapper).selectRecentByTenantId("tenant-1", 50);
    }

    @Test
    void getRecentLogsByTenant_limitTooHigh_fallsBackToDefault() {
        when(auditLogMapper.selectRecentByTenantId("tenant-1", 50)).thenReturn(Collections.emptyList());

        List<SfAuditLog> result = auditLogService.getRecentLogsByTenant("tenant-1", 5000);

        assertThat(result).isEmpty();
        verify(auditLogMapper).selectRecentByTenantId("tenant-1", 50);
    }

    // ------------------------------------------------------------------
    // recordAuditLog
    // ------------------------------------------------------------------

    @Test
    void recordAuditLog_nullExecutedAt_setsNow() {
        SfAuditLog log = new SfAuditLog();
        when(auditLogMapper.insert(any())).thenReturn(1);

        auditLogService.recordAuditLog(log);

        assertThat(log.getExecutedAt()).isNotNull();
        assertThat(log.getStatus()).isEqualTo(1);
        verify(auditLogMapper).insert(log);
    }

    @Test
    void recordAuditLog_preservesProvidedValues() {
        SfAuditLog log = new SfAuditLog();
        log.setExecutedAt(LocalDateTime.now().minusDays(1));
        log.setStatus(0);
        when(auditLogMapper.insert(any())).thenReturn(1);

        auditLogService.recordAuditLog(log);

        assertThat(log.getStatus()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // countFailedActionsSince
    // ------------------------------------------------------------------

    @Test
    void countFailedActionsSince_nullSince_usesDefault() {
        when(auditLogMapper.selectCount(any())).thenReturn(5L);

        long result = auditLogService.countFailedActionsSince(null);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void countFailedActionsSince_withSince_returnsCount() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        when(auditLogMapper.selectCount(any())).thenReturn(3L);

        long result = auditLogService.countFailedActionsSince(since);

        assertThat(result).isEqualTo(3);
    }

    // ------------------------------------------------------------------
    // getRecentFailedLogs
    // ------------------------------------------------------------------

    @Test
    void getRecentFailedLogs_nullLimit_usesDefault() {
        when(auditLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditLog> result = auditLogService.getRecentFailedLogs(null);

        assertThat(result).isEmpty();
    }

    @Test
    void getRecentFailedLogs_withLimit_returnsLogs() {
        when(auditLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditLog> result = auditLogService.getRecentFailedLogs(10);

        assertThat(result).isEmpty();
    }
}
