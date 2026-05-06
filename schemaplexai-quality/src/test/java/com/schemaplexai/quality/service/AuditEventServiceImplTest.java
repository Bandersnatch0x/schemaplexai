package com.schemaplexai.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceImplTest {

    @Mock
    private AuditEventMapper auditEventMapper;

    @InjectMocks
    private AuditEventServiceImpl auditEventService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditEventService, "baseMapper", auditEventMapper);
        ReflectionTestUtils.setField(auditEventService, "objectMapper", new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullEventType_throwsParamError() {
        SfAuditEvent event = new SfAuditEvent();
        event.setResourceType("USER");
        event.setAction("CREATE");

        assertThatThrownBy(() -> auditEventService.save(event))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullResourceType_throwsParamError() {
        SfAuditEvent event = new SfAuditEvent();
        event.setEventType("SECURITY");
        event.setAction("CREATE");

        assertThatThrownBy(() -> auditEventService.save(event))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullAction_throwsParamError() {
        SfAuditEvent event = new SfAuditEvent();
        event.setEventType("SECURITY");
        event.setResourceType("USER");

        assertThatThrownBy(() -> auditEventService.save(event))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success() {
        SfAuditEvent event = new SfAuditEvent();
        event.setEventType("SECURITY");
        event.setResourceType("USER");
        event.setAction("CREATE");
        when(auditEventMapper.insert(any())).thenReturn(1);

        boolean result = auditEventService.save(event);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // logEvent
    // ------------------------------------------------------------------

    @Test
    void logEvent_withDetails_serializesToJson() {
        when(auditEventMapper.insert(any())).thenReturn(1);

        auditEventService.logEvent("SECURITY", "USER", 1L, "CREATE", 10L, Map.of("key", "value"));

        verify(auditEventMapper).insert(argThat(e -> e.getDetailsJson() != null && e.getDetailsJson().contains("key")));
    }

    @Test
    void logEvent_nullDetails_doesNotSetJson() {
        when(auditEventMapper.insert(any())).thenReturn(1);

        auditEventService.logEvent("SECURITY", "USER", 1L, "CREATE", 10L, null);

        verify(auditEventMapper).insert(argThat(e -> e.getDetailsJson() == null));
    }

    // ------------------------------------------------------------------
    // findEventsByResource
    // ------------------------------------------------------------------

    @Test
    void findEventsByResource_returnsEvents() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditEvent> result = auditEventService.findEventsByResource("USER", 1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // findEventsByType
    // ------------------------------------------------------------------

    @Test
    void findEventsByType_returnsEvents() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditEvent> result = auditEventService.findEventsByType("SECURITY");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // findEventsByUser
    // ------------------------------------------------------------------

    @Test
    void findEventsByUser_returnsEvents() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditEvent> result = auditEventService.findEventsByUser(10L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // findEventsInTimeRange
    // ------------------------------------------------------------------

    @Test
    void findEventsInTimeRange_returnsEvents() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<SfAuditEvent> result = auditEventService.findEventsInTimeRange(start, end);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // getEventDetails
    // ------------------------------------------------------------------

    @Test
    void getEventDetails_notFound_throwsNotFound() {
        when(auditEventMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> auditEventService.getEventDetails(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getEventDetails_nullJson_returnsEmptyMap() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);
        when(auditEventMapper.selectById(1L)).thenReturn(event);

        Map<String, Object> result = auditEventService.getEventDetails(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getEventDetails_validJson_returnsMap() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);
        event.setDetailsJson("{\"key\":\"value\"}");
        when(auditEventMapper.selectById(1L)).thenReturn(event);

        Map<String, Object> result = auditEventService.getEventDetails(1L);

        assertThat(result).containsEntry("key", "value");
    }

    @Test
    void getEventDetails_invalidJson_returnsEmptyMap() {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(1L);
        event.setDetailsJson("invalid json");
        when(auditEventMapper.selectById(1L)).thenReturn(event);

        Map<String, Object> result = auditEventService.getEventDetails(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // getActionCountsForResource
    // ------------------------------------------------------------------

    @Test
    void getActionCountsForResource_returnsCounts() {
        SfAuditEvent e1 = new SfAuditEvent();
        e1.setAction("CREATE");
        SfAuditEvent e2 = new SfAuditEvent();
        e2.setAction("CREATE");
        SfAuditEvent e3 = new SfAuditEvent();
        e3.setAction("DELETE");
        when(auditEventMapper.selectList(any())).thenReturn(List.of(e1, e2, e3));

        Map<String, Long> result = auditEventService.getActionCountsForResource("USER", 1L);

        assertThat(result).containsEntry("CREATE", 2L);
        assertThat(result).containsEntry("DELETE", 1L);
    }

    // ------------------------------------------------------------------
    // getLatestEventForResource
    // ------------------------------------------------------------------

    @Test
    void getLatestEventForResource_returnsEvent() {
        SfAuditEvent event = new SfAuditEvent();
        when(auditEventMapper.selectList(any())).thenReturn(List.of(event));

        SfAuditEvent result = auditEventService.getLatestEventForResource("USER", 1L);

        assertThat(result).isEqualTo(event);
    }

    @Test
    void getLatestEventForResource_noEvents_returnsNull() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        SfAuditEvent result = auditEventService.getLatestEventForResource("USER", 1L);

        assertThat(result).isNull();
    }
}
