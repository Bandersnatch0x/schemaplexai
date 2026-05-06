package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, SfNotification> implements NotificationService {

    private static final int STATUS_UNREAD = 0;
    private static final int STATUS_READ = 1;

    @Override
    public SfNotification sendNotification(Long userId, String type, String title, String content) {
        if (userId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "User ID is required");
        }
        if (type == null || type.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Notification type is required");
        }
        if (title == null || title.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Notification title is required");
        }

        SfNotification notification = new SfNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(STATUS_UNREAD);
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            notification.setTenantId(tenantId);
        }
        baseMapper.insert(notification);
        log.info("Sent notification: id={}, userId={}, type={}, title={}",
                notification.getId(), userId, type, title);
        return notification;
    }

    @Override
    public SfNotification markAsRead(Long notificationId) {
        SfNotification notification = baseMapper.selectById(notificationId);
        if (notification == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Notification not found: " + notificationId);
        }
        if (notification.getStatus() != null && notification.getStatus() == STATUS_READ) {
            return notification;
        }
        notification.setStatus(STATUS_READ);
        notification.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(notification);
        log.info("Marked notification as read: id={}, userId={}", notificationId, notification.getUserId());
        return notification;
    }

    @Override
    public List<SfNotification> listUnread(Long userId) {
        if (userId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "User ID is required");
        }
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfNotification::getUserId, userId);
        wrapper.eq(SfNotification::getStatus, STATUS_UNREAD);
        if (tenantId != null) {
            wrapper.eq(SfNotification::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfNotification::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public int batchMarkAsRead(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long id : notificationIds) {
            SfNotification notification = baseMapper.selectById(id);
            if (notification != null && (notification.getStatus() == null || notification.getStatus() != STATUS_READ)) {
                notification.setStatus(STATUS_READ);
                notification.setUpdatedAt(LocalDateTime.now());
                baseMapper.updateById(notification);
                count++;
            }
        }
        log.info("Batch marked {} notifications as read", count);
        return count;
    }
}
