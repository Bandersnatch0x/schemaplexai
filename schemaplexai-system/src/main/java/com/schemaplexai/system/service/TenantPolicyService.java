package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.TenantPolicy;
import com.schemaplexai.system.mapper.TenantPolicyMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPolicyService extends ServiceImpl<TenantPolicyMapper, TenantPolicy> {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public List<TenantPolicy> getPoliciesByTenant(String tenantId) {
        return baseMapper.selectByTenantId(tenantId);
    }

    public TenantPolicy getPolicy(String tenantId, String policyType) {
        return lambdaQuery()
                .eq(TenantPolicy::getTenantId, tenantId)
                .eq(TenantPolicy::getPolicyType, policyType)
                .eq(TenantPolicy::getDeleted, 0)
                .one();
    }

    public void saveOrUpdatePolicy(String tenantId, String policyType, String configJson) {
        TenantPolicy existing = getPolicy(tenantId, policyType);
        if (existing != null) {
            existing.setConfigJson(configJson);
            existing.setEnabled(true);
            boolean updated = updateById(existing);
            if (!updated) {
                throw new BaseException(ResultCode.CONFLICT,
                        "TenantPolicy concurrent modification detected for tenant=" + tenantId + ", type=" + policyType);
            }
            log.info("Updated tenant policy: tenantId={}, policyType={}", tenantId, policyType);
        } else {
            TenantPolicy policy = new TenantPolicy();
            policy.setTenantId(tenantId);
            policy.setPolicyType(policyType);
            policy.setConfigJson(configJson);
            policy.setEnabled(true);
            policy.setVersion(0);
            save(policy);
            log.info("Created tenant policy: tenantId={}, policyType={}", tenantId, policyType);
        }
        publishPolicyUpdatedEvent(tenantId, policyType);
    }

    @SneakyThrows
    private void publishPolicyUpdatedEvent(String tenantId, String policyType) {
        Map<String, Object> payload = Map.of(
                "tenantId", tenantId,
                "policyType", policyType,
                "timestamp", System.currentTimeMillis()
        );
        String message = objectMapper.writeValueAsString(payload);
        rabbitTemplate.convertAndSend(
                CommonConstants.EXCHANGE_SCHEMAPLEXAI,
                CommonConstants.RK_TENANT_POLICY_UPDATED,
                message
        );
        log.debug("Published tenant policy updated event: tenantId={}, policyType={}", tenantId, policyType);
    }
}
