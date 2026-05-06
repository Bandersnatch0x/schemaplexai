package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class AuditEventServiceImpl extends ServiceImpl<AuditEventMapper, SfAuditEvent> implements AuditEventService {

    private final ObjectMapper objectMapper;

    /**
     * Log a new audit event with validation.
     */
    @Override
    public boolean save(SfAuditEvent event) {
        validateEvent(event);
        log.debug("Logging audit event: type={}, resourceType={}, action={}",
            event.getEventType(), event.getResourceType(), event.getAction());
        return super.save(event);
    }

    /**
     * Log an audit event with structured details.
     */
    public void logEvent(String eventType, String resourceType, Long resourceId,
                         String action, Long userId, Map<String, Object> details) {
        SfAuditEvent event = new SfAuditEvent();
        event.setEventType(eventType);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setAction(action);
        event.setUserId(userId);
        if (details != null && !details.isEmpty()) {
            try {
                event.setDetailsJson(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                log.warn("Failed to serialize audit event details, storing empty", e);
                event.setDetailsJson("{}");
            }
        }
        save(event);
        log.info("Audit event logged: type={}, resource={}:{}, action={}, user={}",
            eventType, resourceType, resourceId, action, userId);
    }

    /**
     * Query audit events for a specific resource.
     */
    public List<SfAuditEvent> findEventsByResource(String resourceType, Long resourceId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfAuditEvent>()
                .eq(SfAuditEvent::getResourceType, resourceType)
                .eq(SfAuditEvent::getResourceId, resourceId)
                .orderByDesc(SfAuditEvent::getCreatedAt));
    }

    /**
     * Query audit events by event type.
     */
    public List<SfAuditEvent> findEventsByType(String eventType) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfAuditEvent>()
                .eq(SfAuditEvent::getEventType, eventType)
                .orderByDesc(SfAuditEvent::getCreatedAt));
    }

    /**
     * Query audit events by user.
     */
    public List<SfAuditEvent> findEventsByUser(Long userId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfAuditEvent>()
                .eq(SfAuditEvent::getUserId, userId)
                .orderByDesc(SfAuditEvent::getCreatedAt));
    }

    /**
     * Query audit events within a time range.
     */
    public List<SfAuditEvent> findEventsInTimeRange(LocalDateTime start, LocalDateTime end) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfAuditEvent>()
                .ge(SfAuditEvent::getCreatedAt, start)
                .le(SfAuditEvent::getCreatedAt, end)
                .orderByDesc(SfAuditEvent::getCreatedAt));
    }

    /**
     * Get event details as a Map.
     */
    public Map<String, Object> getEventDetails(Long eventId) {
        SfAuditEvent event = baseMapper.selectById(eventId);
        if (event == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Audit event not found: " + eventId);
        }
        if (event.getDetailsJson() == null || event.getDetailsJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(event.getDetailsJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse audit event details for event {}", eventId, e);
            return Map.of();
        }
    }

    /**
     * Get aggregated event counts by action for a resource.
     */
    public Map<String, Long> getActionCountsForResource(String resourceType, Long resourceId) {
        List<SfAuditEvent> events = findEventsByResource(resourceType, resourceId);
        return events.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getAction() != null ? e.getAction() : "UNKNOWN",
                java.util.stream.Collectors.counting()));
    }

    /**
     * Get the most recent event for a resource.
     */
    public SfAuditEvent getLatestEventForResource(String resourceType, Long resourceId) {
        List<SfAuditEvent> events = baseMapper.selectList(
            new LambdaQueryWrapper<SfAuditEvent>()
                .eq(SfAuditEvent::getResourceType, resourceType)
                .eq(SfAuditEvent::getResourceId, resourceId)
                .orderByDesc(SfAuditEvent::getCreatedAt)
                .last("LIMIT 1"));
        return events.isEmpty() ? null : events.get(0);
    }

    private void validateEvent(SfAuditEvent event) {
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Event type is required");
        }
        if (event.getResourceType() == null || event.getResourceType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Resource type is required");
        }
        if (event.getAction() == null || event.getAction().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Action is required");
        }
    }
}
