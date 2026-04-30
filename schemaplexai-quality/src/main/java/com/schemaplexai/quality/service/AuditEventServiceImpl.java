package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class AuditEventServiceImpl extends ServiceImpl<AuditEventMapper, SfAuditEvent> implements AuditEventService {
}
