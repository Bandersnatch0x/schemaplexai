package com.schemaplexai.web.service.notification;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.web.vo.notification.NotificationVO;

public interface NotificationService {

    IPage<NotificationVO> pageQuery(Long userId, Integer page, Integer size, Boolean read);

    boolean markAsRead(Long id, Long userId);

    int markAllAsRead(Long userId);

    void sendNotification(Notification notification);
}
