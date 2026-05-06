package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpecSteering;
import com.schemaplexai.spec.mapper.SfSpecSteeringMapper;
import com.schemaplexai.spec.service.SpecSteeringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SpecSteeringServiceImpl extends ServiceImpl<SfSpecSteeringMapper, SfSpecSteering> implements SpecSteeringService {

    private final SfSpecSteeringMapper specSteeringMapper;

    @Override
    public Map<String, Boolean> evaluateSteeringRules(Long specId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Content must not be empty");
        }

        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecSteering> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecSteering::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecSteering::getTenantId, tenantId);
        }
        List<SfSpecSteering> steerings = specSteeringMapper.selectList(wrapper);

        Map<String, Boolean> results = new HashMap<>();
        for (SfSpecSteering steering : steerings) {
            boolean directionMatch = steering.getDirection() != null && content.contains(steering.getDirection());
            boolean constraintsMatch = steering.getConstraints() != null && content.contains(steering.getConstraints());
            boolean criteriaMatch = steering.getAcceptanceCriteria() != null && content.contains(steering.getAcceptanceCriteria());
            results.put("direction_" + steering.getId(), directionMatch);
            results.put("constraints_" + steering.getId(), constraintsMatch);
            results.put("criteria_" + steering.getId(), criteriaMatch);
        }

        log.info("Evaluated {} steering rules for spec {}", steerings.size(), specId);
        return results;
    }

    @Override
    public String applySteering(Long specId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Content must not be empty");
        }

        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecSteering> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecSteering::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecSteering::getTenantId, tenantId);
        }
        List<SfSpecSteering> steerings = specSteeringMapper.selectList(wrapper);

        StringBuilder guided = new StringBuilder(content);
        for (SfSpecSteering steering : steerings) {
            if (steering.getDirection() != null && !guided.toString().contains(steering.getDirection())) {
                guided.append("\n<!-- Direction: ").append(steering.getDirection()).append(" -->");
            }
            if (steering.getConstraints() != null && !guided.toString().contains(steering.getConstraints())) {
                guided.append("\n<!-- Constraints: ").append(steering.getConstraints()).append(" -->");
            }
            if (steering.getAcceptanceCriteria() != null && !guided.toString().contains(steering.getAcceptanceCriteria())) {
                guided.append("\n<!-- Acceptance Criteria: ").append(steering.getAcceptanceCriteria()).append(" -->");
            }
        }

        log.info("Applied steering for spec {} with {} rules", specId, steerings.size());
        return guided.toString();
    }

    @Override
    public List<SfSpecSteering> listActiveSteerings(Long specId) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecSteering> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecSteering::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecSteering::getTenantId, tenantId);
        }
        wrapper.and(w -> w.isNotNull(SfSpecSteering::getDirection)
                .or().isNotNull(SfSpecSteering::getConstraints)
                .or().isNotNull(SfSpecSteering::getAcceptanceCriteria));
        wrapper.orderByAsc(SfSpecSteering::getCreatedAt);
        return specSteeringMapper.selectList(wrapper);
    }

    @Override
    public boolean validateSteeringConfig(Long steeringId) {
        SfSpecSteering steering = specSteeringMapper.selectById(steeringId);
        if (steering == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Steering not found");
        }
        boolean hasDirection = StringUtils.hasText(steering.getDirection());
        boolean hasConstraints = StringUtils.hasText(steering.getConstraints());
        boolean hasCriteria = StringUtils.hasText(steering.getAcceptanceCriteria());
        boolean valid = hasDirection || hasConstraints || hasCriteria;
        log.info("Validated steering {}: valid={}", steeringId, valid);
        return valid;
    }
}
