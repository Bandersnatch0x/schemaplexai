package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.ops.entity.SfNotification;

import java.util.List;

public interface NotificationService extends IService<SfNotification> {

    /**
     * Send a notification to a user.
     *
     * @param userId  the user ID
     * @param type    the notification type
     * @param title   the notification title
     * @param content the notification content
     * @return the created notification
     */
    SfNotification sendNotification(Long userId, String type, String title, String content);

    /**
     * Mark a notification as read.
     *
     * @param notificationId the notification ID
     * @return the updated notification
     */
    SfNotification markAsRead(Long notificationId);

    /**
     * List unread notifications for a user.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    List<SfNotification> listUnread(Long userId);

    /**
     * Batch mark notifications as read.
     *
     * @param notificationIds the notification IDs
     * @return number of notifications marked as read
     */
    int batchMarkAsRead(List<Long> notificationIds);

    /**
     * List persisted budget alert records (notification type
     * {@link BudgetAlertNotifier#ALERT_NOTIFICATION_TYPE}) for one tenant,
     * newest first. Backs {@code GET /ops/budgets/alerts} (cost-analytics
     * spec §4.2).
     *
     * <p>Review ST-01: the tenant filter is mandatory. A null/blank tenant
     * fails closed with a parameter error instead of returning all tenants'
     * alert records.
     *
     * @param tenantId the tenant to list alerts for (required, non-blank)
     * @return the budget alert records of that tenant
     */
    List<SfNotification> listBudgetAlerts(String tenantId);
}
