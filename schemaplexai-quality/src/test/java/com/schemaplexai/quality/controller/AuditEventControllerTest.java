package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
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
class AuditEventControllerTest {

    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private AuditEventController auditEventController;

    @Test
    void create_returnsId() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);

        Result<Long> result = auditEventController.create(event);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(auditEventService).save(event);
    }

    @Test
    void update_returnsBoolean() {
        SfAuditEvent event = new SfAuditEvent();
        when(auditEventService.updateById(any())).thenReturn(true);

        Result<Boolean> result = auditEventController.update(1L, event);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(event.getId()).isEqualTo(1L);
        verify(auditEventService).updateById(event);
    }

    @Test
    void update_returnsFalse_whenServiceFails() {
        SfAuditEvent event = new SfAuditEvent();
        when(auditEventService.updateById(any())).thenReturn(false);

        Result<Boolean> result = auditEventController.update(1L, event);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void delete_returnsBoolean() {
        when(auditEventService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = auditEventController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(auditEventService).removeById(1L);
    }

    @Test
    void delete_returnsFalse_whenServiceFails() {
        when(auditEventService.removeById(1L)).thenReturn(false);

        Result<Boolean> result = auditEventController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void get_found() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);
        when(auditEventService.getById(1L)).thenReturn(event);

        Result<SfAuditEvent> result = auditEventController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(event);
    }

    @Test
    void get_notFound() {
        when(auditEventService.getById(1L)).thenReturn(null);

        Result<SfAuditEvent> result = auditEventController.get(1L);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void list_returnsEvents() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);
        when(auditEventService.list()).thenReturn(List.of(event));

        Result<List<SfAuditEvent>> result = auditEventController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void list_returnsEmptyList() {
        when(auditEventService.list()).thenReturn(Collections.emptyList());

        Result<List<SfAuditEvent>> result = auditEventController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
