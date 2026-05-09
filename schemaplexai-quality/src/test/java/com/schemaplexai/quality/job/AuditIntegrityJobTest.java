package com.schemaplexai.quality.job;

import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4.4: Audit Integrity Job Tests")
class AuditIntegrityJobTest {

    @Mock
    private AuditEventMapper auditEventMapper;

    @InjectMocks
    private AuditIntegrityJob auditIntegrityJob;

    @Test
    @DisplayName("Flags corrupted events with mismatched hash")
    void flagsCorruptedEvents() {
        SfAuditEvent good = createEvent("TOOL_CALLED", "{\"tool\":\"git.push\"}");
        SfAuditEvent bad = createEvent("TOOL_CALLED", "{\"tampered\":true}");
        // Manually set a hash that won't match
        bad.setContentHash("invalidhash");

        when(auditEventMapper.selectRecentEvents(any())).thenReturn(List.of(good, bad));

        auditIntegrityJob.runIntegrityCheck();

        verify(auditEventMapper).markCorrupted(bad.getId());
        verify(auditEventMapper, never()).markCorrupted(good.getId());
    }

    @Test
    @DisplayName("Handles empty event list gracefully")
    void handlesEmptyList() {
        when(auditEventMapper.selectRecentEvents(any())).thenReturn(List.of());

        auditIntegrityJob.runIntegrityCheck();

        verify(auditEventMapper, never()).markCorrupted(any());
    }

    private SfAuditEvent createEvent(String type, String payload) {
        SfAuditEvent event = new SfAuditEvent();
        event.setId(System.currentTimeMillis());
        event.setEventType(type);
        event.setDetailsJson(payload);
        event.setOccurredAt(LocalDateTime.now());
        event.setCorrupted(false);
        event.setEventId(UUID.randomUUID());
        return event;
    }
}
