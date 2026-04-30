package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, SfNotification> implements NotificationService {
}
