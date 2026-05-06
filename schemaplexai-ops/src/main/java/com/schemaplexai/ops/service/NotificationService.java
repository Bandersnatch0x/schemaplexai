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
}
