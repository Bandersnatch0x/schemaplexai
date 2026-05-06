package com.schemaplexai.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications for spec review workflow events.
 * Handles approval, rejection, and escalation notifications.
 * In production, this would integrate with email/SMS/notification service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecReviewNotificationDelegate implements JavaDelegate {

    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();
        String tenantId = resolveTenantId(execution);

        String specId = (String) execution.getVariable("specId");
        String specTitle = (String) execution.getVariable("specTitle");
        String submitterId = (String) execution.getVariable("submitterId");
        String approvalDecision = (String) execution.getVariable("approvalDecision");
        String rejectionReason = (String) execution.getVariable("rejectionReason");

        Map<String, Object> notification = new HashMap<>();
        notification.put("processInstanceId", processInstanceId);
        notification.put("tenantId", tenantId);
        notification.put("specId", specId);
        notification.put("specTitle", specTitle != null ? specTitle : "Untitled Spec");
        notification.put("submitterId", submitterId);
        notification.put("timestamp", Instant.now().toString());

        switch (activityId) {
            case "autoApproveTask" -> {
                notification.put("eventType", "AUTO_APPROVED");
                notification.put("message", "Spec '" + specTitle + "' has been auto-approved (low risk).");
                execution.setVariable("finalStatus", "AUTO_APPROVED");
                log.info("[SpecReviewNotify] Auto-approved spec={} tenant={}", specId, tenantId);
            }
            case "notifyApprovalTask" -> {
                notification.put("eventType", "APPROVED");
                notification.put("message", "Spec '" + specTitle + "' has been approved.");
                execution.setVariable("finalStatus", "APPROVED");
                execution.setVariable("approvedAt", Instant.now().toString());
                log.info("[SpecReviewNotify] Approved spec={} tenant={} decision={}",
                        specId, tenantId, approvalDecision);
            }
            case "notifyRejectionTask" -> {
                notification.put("eventType", "REJECTED");
                notification.put("message", "Spec '" + specTitle + "' has been rejected.");
                notification.put("rejectionReason", rejectionReason != null ? rejectionReason : "No reason provided");
                execution.setVariable("finalStatus", "REJECTED");
                execution.setVariable("rejectedAt", Instant.now().toString());
                log.info("[SpecReviewNotify] Rejected spec={} tenant={} reason={}",
                        specId, tenantId, rejectionReason);
            }
            default -> {
                notification.put("eventType", "UNKNOWN");
                notification.put("message", "Unknown notification event for activity: " + activityId);
                log.warn("[SpecReviewNotify] Unknown activityId={} spec={}", activityId, specId);
            }
        }

        try {
            String notificationJson = objectMapper.writeValueAsString(notification);
            execution.setVariable("lastNotification", notificationJson);
            // TODO: integrate with notification service (e.g., RabbitMQ, WebSocket, email)
            log.debug("[SpecReviewNotify] Notification payload: {}", notificationJson);
        } catch (Exception e) {
            log.warn("[SpecReviewNotify] Failed to serialize notification", e);
        }
    }

    private String resolveTenantId(DelegateExecution execution) {
        String tenantId = (String) execution.getVariable("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }
        }
        return tenantId;
    }
}
