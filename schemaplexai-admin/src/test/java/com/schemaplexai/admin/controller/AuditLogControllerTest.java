package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.AuditLogQuery;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.service.AuditLogService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    @Test
    void page_returnsAuditLogs() {
        AuditLogQuery query = new AuditLogQuery();
        PageResult<SfAuditLog> pageResult = PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        when(auditLogService.queryAuditLogs(query)).thenReturn(pageResult);

        Result<PageResult<SfAuditLog>> result = auditLogController.page(query);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(pageResult);
        verify(auditLogService).queryAuditLogs(query);
    }

    @Test
    void getById_found() {
        SfAuditLog logEntry = new SfAuditLog();
        logEntry.setId(1L);
        when(auditLogService.getById(1L)).thenReturn(logEntry);

        Result<SfAuditLog> result = auditLogController.getById(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(logEntry);
    }

    @Test
    void getById_notFound() {
        when(auditLogService.getById(1L)).thenReturn(null);

        Result<SfAuditLog> result = auditLogController.getById(1L);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("audit log not found");
    }

    @Test
    void recentByTenant_returnsLogs() {
        SfAuditLog logEntry = new SfAuditLog();
        logEntry.setId(1L);
        when(auditLogService.getRecentLogsByTenant("tenant1", 50)).thenReturn(List.of(logEntry));

        Result<List<SfAuditLog>> result = auditLogController.recentByTenant("tenant1", 50);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void create_returnsId() {
        SfAuditLog auditLog = new SfAuditLog();
        auditLog.setId(1L);

        Result<Long> result = auditLogController.create(auditLog);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(auditLogService).recordAuditLog(auditLog);
    }

    @Test
    void recentFailed_returnsLogs() {
        SfAuditLog logEntry = new SfAuditLog();
        logEntry.setId(1L);
        when(auditLogService.getRecentFailedLogs(20)).thenReturn(List.of(logEntry));

        Result<List<SfAuditLog>> result = auditLogController.recentFailed(20);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }
}
