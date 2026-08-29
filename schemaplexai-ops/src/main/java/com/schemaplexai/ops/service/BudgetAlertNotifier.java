package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dispatches budget alerts through the notification chain (issue 921 / REQ-09).
 * <p>
 * Dual-track delivery, per the cost-analytics spec ("超过阈值 → 发送告警通知"):
 * <ol>
 *   <li><b>Event persistence</b>: every alert is written to {@code sf_notification}
 *       (type {@value #ALERT_NOTIFICATION_TYPE}), which also serves as the alert
 *       record store for {@code GET /ops/budgets/alerts} (REQ-11).</li>
 *   <li><b>Notification chain</b>: best-effort publish to the unified notification
 *       MQ route ({@code sf.exchange} / {@code sf.notification}) so the task-module
 *       NotificationConsumer can deliver it in-app. A broker failure never breaks
 *       the alert check — the persisted record plus log remain.</li>
 * </ol>
 * <p>
 * Alerts are de-duplicated per budget and level within a 24h window so the hourly
 * schedule does not spam while a breach persists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertNotifier {

    /** Notification type used for persisted budget alert records. */
    public static final String ALERT_NOTIFICATION_TYPE = "BUDGET_ALERT";
    /** Alert level: budget usage reached 100% of the limit. */
    public static final String LEVEL_EXCEEDED = "EXCEEDED";
    /** Alert level: budget usage reached the configured alert threshold. */
    public static final String LEVEL_THRESHOLD_REACHED = "THRESHOLD_REACHED";
    /** Deduplication window: one notification per budget+level per day. */
    static final int DEDUP_WINDOW_HOURS = 24;
    /**
     * Recipient sentinel for tenant-level alerts: sf_notification.user_id is NOT NULL
     * and budgets are not bound to individual users in this schema.
     */
    static final long BROADCAST_USER_ID = 0L;

    private final NotificationMapper notificationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Persist and publish a budget alert unless the same budget+level was already
     * alerted within the deduplication window.
     *
     * @param budget       the breached budget
     * @param alertLevel   {@link #LEVEL_EXCEEDED} or {@link #LEVEL_THRESHOLD_REACHED}
     * @param usagePercent usage as a percentage of the limit (for reporting)
     * @return true when a new alert was dispatched, false when deduplicated away
     */
    public boolean dispatchBudgetAlert(SfBudget budget, String alertLevel, BigDecimal usagePercent) {
        String title = buildTitle(budget, alertLevel);

        if (isDuplicate(budget, title)) {
            log.debug("Budget alert suppressed (already dispatched within {}h): {}",
                    DEDUP_WINDOW_HOURS, title);
            return false;
        }

        String content = String.format(
                "Budget %s (tenant=%s) usage reached %s%%: used=%s, limit=%s, threshold=%s, level=%s",
                budgetIdentifier(budget), budget.getTenantId(), usagePercent,
                budget.getUsedAmount(), budget.getLimitAmount(), budget.getAlertThreshold(), alertLevel);

        persistAlertRecord(budget, title, content);
        publishNotificationEvent(budget, title, content);
        return true;
    }

    private String buildTitle(SfBudget budget, String alertLevel) {
        return "Budget alert [" + alertLevel + "] " + budgetIdentifier(budget);
    }

    private String budgetIdentifier(SfBudget budget) {
        return budget.getId() != null
                ? "budgetId=" + budget.getId()
                : "budgetType=" + budget.getBudgetType();
    }

    private boolean isDuplicate(SfBudget budget, String title) {
        LambdaQueryWrapper<SfNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfNotification::getType, ALERT_NOTIFICATION_TYPE)
                .eq(SfNotification::getTitle, title)
                .ge(SfNotification::getCreatedAt, LocalDateTime.now().minusHours(DEDUP_WINDOW_HOURS));
        if (budget.getTenantId() != null) {
            wrapper.eq(SfNotification::getTenantId, budget.getTenantId());
        }
        Long count = notificationMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private void persistAlertRecord(SfBudget budget, String title, String content) {
        SfNotification notification = new SfNotification();
        notification.setTenantId(budget.getTenantId());
        notification.setUserId(BROADCAST_USER_ID);
        notification.setType(ALERT_NOTIFICATION_TYPE);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRead(false);
        notificationMapper.insert(notification);
        log.info("Budget alert record persisted: {}", title);
    }

    private void publishNotificationEvent(SfBudget budget, String title, String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("channel", "in-app");
            payload.put("tenantId", budget.getTenantId());
            payload.put("userId", BROADCAST_USER_ID);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("idempotencyKey", "budget-alert:" + budgetIdentifier(budget) + ":"
                    + java.time.LocalDate.now());
            rabbitTemplate.convertAndSend(
                    CommonConstants.EXCHANGE_SCHEMAPLEXAI,
                    CommonConstants.RK_NOTIFICATION,
                    objectMapper.writeValueAsString(payload));
            log.info("Budget alert published to notification chain: {}", title);
        } catch (Exception e) {
            // Dual-track: the alert record is already persisted and logged, so a broker
            // outage must not fail the hourly alert check.
            log.warn("Budget alert MQ publish failed; alert persisted to sf_notification only: {}",
                    title, e);
        }
    }
}
