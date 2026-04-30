package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.mapper.DeliveryRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class DeliveryServiceImpl extends ServiceImpl<DeliveryRecordMapper, SfDeliveryRecord> implements DeliveryService {
}
