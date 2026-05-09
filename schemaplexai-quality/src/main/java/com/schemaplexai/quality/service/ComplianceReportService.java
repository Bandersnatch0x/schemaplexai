package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceReportService {

    private final AuditEventMapper auditEventMapper;

    public Map<String, Object> buildExecutionReport(Long executionId) {
        List<SfAuditEvent> events = auditEventMapper.selectList(
                new LambdaQueryWrapper<SfAuditEvent>()
                        .eq(SfAuditEvent::getExecutionId, executionId)
                        .orderByAsc(SfAuditEvent::getOccurredAt));

        Map<String, Object> report = new HashMap<>();
        report.put("executionId", executionId);
        report.put("eventCount", events.size());
        report.put("events", events.stream().map(this::toSummary).toList());
        report.put("corruptedCount", events.stream().filter(e -> Boolean.TRUE.equals(e.getCorrupted())).count());
        report.put("generatedAt", LocalDateTime.now());
        return report;
    }

    public List<SfAuditEvent> queryAuditEvents(String eventType, String resourceType,
                                                  Long resourceId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SfAuditEvent> wrapper = new LambdaQueryWrapper<>();
        if (eventType != null) wrapper.eq(SfAuditEvent::getEventType, eventType);
        if (resourceType != null) wrapper.eq(SfAuditEvent::getResourceType, resourceType);
        if (resourceId != null) wrapper.eq(SfAuditEvent::getResourceId, resourceId);
        if (start != null) wrapper.ge(SfAuditEvent::getOccurredAt, start);
        if (end != null) wrapper.le(SfAuditEvent::getOccurredAt, end);
        wrapper.orderByDesc(SfAuditEvent::getOccurredAt);
        return auditEventMapper.selectList(wrapper);
    }

    public Map<String, Object> buildDashboard() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<SfAuditEvent> recent = auditEventMapper.selectRecentEvents(since);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("last24hEventCount", recent.size());
        dashboard.put("corruptedEvents", recent.stream().filter(e -> Boolean.TRUE.equals(e.getCorrupted())).count());
        dashboard.put("eventTypeBreakdown", recent.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SfAuditEvent::getEventType, java.util.stream.Collectors.counting())));
        dashboard.put("generatedAt", LocalDateTime.now());
        return dashboard;
    }

    private Map<String, Object> toSummary(SfAuditEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", event.getEventId());
        map.put("eventType", event.getEventType());
        map.put("occurredAt", event.getOccurredAt());
        map.put("corrupted", event.getCorrupted());
        return map;
    }
}
