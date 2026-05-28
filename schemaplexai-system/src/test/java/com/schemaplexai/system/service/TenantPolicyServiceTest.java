package com.schemaplexai.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.TenantPolicy;
import com.schemaplexai.system.mapper.TenantPolicyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantPolicyServiceTest {

    @Mock
    private TenantPolicyMapper tenantPolicyMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TenantPolicyService tenantPolicyService;

    @BeforeEach
    void setUp() {
        tenantPolicyService = new TenantPolicyService(rabbitTemplate, objectMapper);
        ReflectionTestUtils.setField(tenantPolicyService, "baseMapper", tenantPolicyMapper);
    }

    @Test
    void getPoliciesByTenant_returnsPolicies() {
        TenantPolicy policy1 = createPolicy(1L, "T1", "APPROVAL_THRESHOLD", "{\"threshold\":100}");
        TenantPolicy policy2 = createPolicy(2L, "T1", "TOOL_WHITELIST", "[\"toolA\"]");
        when(tenantPolicyMapper.selectByTenantId("T1")).thenReturn(List.of(policy1, policy2));

        List<TenantPolicy> result = tenantPolicyService.getPoliciesByTenant("T1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantPolicy::getPolicyType)
                .containsExactly("APPROVAL_THRESHOLD", "TOOL_WHITELIST");
    }

    @Test
    void getPoliciesByTenant_returnsEmptyListWhenNoPolicies() {
        when(tenantPolicyMapper.selectByTenantId("T99")).thenReturn(List.of());

        List<TenantPolicy> result = tenantPolicyService.getPoliciesByTenant("T99");

        assertThat(result).isEmpty();
    }

    @Test
    void getPolicy_existingPolicy_returnsPolicy() {
        TenantPolicy policy = createPolicy(1L, "T1", "COST_CAP", "{\"max\":500}");
        when(tenantPolicyMapper.selectOne(any())).thenReturn(policy);

        TenantPolicy result = tenantPolicyService.getPolicy("T1", "COST_CAP");

        assertThat(result).isNotNull();
        assertThat(result.getPolicyType()).isEqualTo("COST_CAP");
        assertThat(result.getConfigJson()).isEqualTo("{\"max\":500}");
    }

    @Test
    void getPolicy_nonExistentPolicy_returnsNull() {
        when(tenantPolicyMapper.selectOne(any())).thenReturn(null);

        TenantPolicy result = tenantPolicyService.getPolicy("T1", "MISSING");

        assertThat(result).isNull();
    }

    @Test
    void saveOrUpdatePolicy_blankTenantId_throwsParamErrorWithoutWriting() {
        assertThatThrownBy(() -> tenantPolicyService.saveOrUpdatePolicy(" ", "RATE_LIMIT", "{\"rpm\":60}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(tenantPolicyMapper, never()).insert(any(TenantPolicy.class));
        verify(tenantPolicyMapper, never()).updateById(any(TenantPolicy.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void saveOrUpdatePolicy_blankPolicyType_throwsParamErrorWithoutWriting() {
        assertThatThrownBy(() -> tenantPolicyService.saveOrUpdatePolicy("T1", " ", "{\"rpm\":60}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(tenantPolicyMapper, never()).insert(any(TenantPolicy.class));
        verify(tenantPolicyMapper, never()).updateById(any(TenantPolicy.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void saveOrUpdatePolicy_newPolicy_insertsAndPublishesEvent() {
        when(tenantPolicyMapper.selectOne(any())).thenReturn(null);
        when(tenantPolicyMapper.insert(any(TenantPolicy.class))).thenReturn(1);

        tenantPolicyService.saveOrUpdatePolicy("T1", "RATE_LIMIT", "{\"rpm\":60}");

        ArgumentCaptor<TenantPolicy> captor = ArgumentCaptor.forClass(TenantPolicy.class);
        verify(tenantPolicyMapper).insert(captor.capture());
        TenantPolicy inserted = captor.getValue();
        assertThat(inserted.getTenantId()).isEqualTo("T1");
        assertThat(inserted.getPolicyType()).isEqualTo("RATE_LIMIT");
        assertThat(inserted.getConfigJson()).isEqualTo("{\"rpm\":60}");
        assertThat(inserted.getEnabled()).isTrue();
        assertThat(inserted.getVersion()).isEqualTo(0);

        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_TENANT_POLICY_UPDATED),
                argThat((String msg) -> msg.contains("T1") && msg.contains("RATE_LIMIT"))
        );
    }

    @Test
    void saveOrUpdatePolicy_existingPolicy_updatesAndPublishesEvent() {
        TenantPolicy existing = createPolicy(5L, "T2", "COST_CAP", "{\"old\":true}");
        existing.setVersion(3);
        when(tenantPolicyMapper.selectOne(any())).thenReturn(existing);
        when(tenantPolicyMapper.updateById(any(TenantPolicy.class))).thenReturn(1);

        tenantPolicyService.saveOrUpdatePolicy("T2", "COST_CAP", "{\"new\":true}");

        ArgumentCaptor<TenantPolicy> captor = ArgumentCaptor.forClass(TenantPolicy.class);
        verify(tenantPolicyMapper).updateById(captor.capture());
        TenantPolicy updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(5L);
        assertThat(updated.getConfigJson()).isEqualTo("{\"new\":true}");
        assertThat(updated.getEnabled()).isTrue();

        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_TENANT_POLICY_UPDATED),
                anyString()
        );
    }

    @Test
    void saveOrUpdatePolicy_concurrentModification_throwsConflict() {
        TenantPolicy existing = createPolicy(5L, "T2", "COST_CAP", "{\"old\":true}");
        existing.setVersion(2);
        when(tenantPolicyMapper.selectOne(any())).thenReturn(existing);
        when(tenantPolicyMapper.updateById(any(TenantPolicy.class))).thenReturn(0);

        assertThatThrownBy(() -> tenantPolicyService.saveOrUpdatePolicy("T2", "COST_CAP", "{\"new\":true}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    private TenantPolicy createPolicy(Long id, String tenantId, String policyType, String configJson) {
        TenantPolicy policy = new TenantPolicy();
        policy.setId(id);
        policy.setTenantId(tenantId);
        policy.setPolicyType(policyType);
        policy.setConfigJson(configJson);
        policy.setEnabled(true);
        policy.setVersion(0);
        return policy;
    }
}
