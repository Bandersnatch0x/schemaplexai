package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.mapper.ArtifactMapper;
import com.schemaplexai.ops.mapper.DeliveryRecordMapper;
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
public class DeliveryServiceImpl extends ServiceImpl<DeliveryRecordMapper, SfDeliveryRecord> implements DeliveryService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_IN_TRANSIT = 1;
    private static final int STATUS_DELIVERED = 2;
    private static final int STATUS_FAILED = 3;

    private final ArtifactMapper artifactMapper;

    @Override
    public SfDeliveryRecord createDelivery(Long artifactId, String deliveryType, String recipient) {
        if (artifactId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact ID is required");
        }
        if (deliveryType == null || deliveryType.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Delivery type is required");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Recipient is required");
        }
        SfArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Artifact not found: " + artifactId);
        }

        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setArtifactId(artifactId);
        record.setDeliveryType(deliveryType);
        record.setRecipient(recipient);
        record.setStatus(STATUS_PENDING);
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            record.setTenantId(tenantId);
        }
        baseMapper.insert(record);
        log.info("Created delivery: id={}, artifactId={}, type={}, recipient={}",
                record.getId(), artifactId, deliveryType, recipient);
        return record;
    }

    @Override
    public SfDeliveryRecord trackDelivery(Long deliveryId, Integer status) {
        if (status == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Status is required");
        }
        SfDeliveryRecord record = baseMapper.selectById(deliveryId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Delivery record not found: " + deliveryId);
        }
        record.setStatus(status);
        record.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("Updated delivery status: id={}, status={}", deliveryId, status);
        return record;
    }

    @Override
    public SfDeliveryRecord confirmDelivery(Long deliveryId) {
        SfDeliveryRecord record = baseMapper.selectById(deliveryId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Delivery record not found: " + deliveryId);
        }
        if (record.getStatus() != null && record.getStatus() == STATUS_DELIVERED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Delivery is already confirmed: " + deliveryId);
        }
        record.setStatus(STATUS_DELIVERED);
        record.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("Confirmed delivery: id={}, artifactId={}", deliveryId, record.getArtifactId());
        return record;
    }

    @Override
    public List<SfDeliveryRecord> listByStatus(Integer status) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfDeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SfDeliveryRecord::getStatus, status);
        }
        if (tenantId != null) {
            wrapper.eq(SfDeliveryRecord::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfDeliveryRecord::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }
}
