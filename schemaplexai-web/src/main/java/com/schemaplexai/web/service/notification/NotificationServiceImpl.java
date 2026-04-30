package com.schemaplexai.web.service.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.web.vo.notification.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public IPage<NotificationVO> pageQuery(Long userId, Integer page, Integer size, Boolean read) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (read != null) {
            wrapper.eq(Notification::getRead, read);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> pageParam = new Page<>(page, size);
        IPage<Notification> entityPage = notificationMapper.selectPage(pageParam, wrapper);

        List<NotificationVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<NotificationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        voPage.setPages(entityPage.getPages());

        return voPage;
    }

    @Override
    public boolean markAsRead(Long id, Long userId) {
        int affected = notificationMapper.markAsRead(id, userId);
        if (affected == 0) {
            throw new BaseException(ResultCode.NOT_FOUND.getCode(), "通知不存在或无权访问");
        }
        return true;
    }

    @Override
    public int markAllAsRead(Long userId) {
        return notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void sendNotification(Notification notification) {
        notification.setRead(false);
        notificationMapper.insert(notification);
    }

    private NotificationVO convertToVO(Notification entity) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
