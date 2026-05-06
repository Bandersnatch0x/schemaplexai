package com.schemaplexai.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.mapper.SecurityPolicyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityPolicyServiceImplTest {

    @Mock
    private SecurityPolicyMapper securityPolicyMapper;

    @InjectMocks
    private SecurityPolicyServiceImpl securityPolicyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityPolicyService, "baseMapper", securityPolicyMapper);
        ReflectionTestUtils.setField(securityPolicyService, "objectMapper", new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullName_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setPolicyType("ACCESS");

        assertThatThrownBy(() -> securityPolicyService.save(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_blankName_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("  ");
        policy.setPolicyType("ACCESS");

        assertThatThrownBy(() -> securityPolicyService.save(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nameTooLong_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("a".repeat(129));
        policy.setPolicyType("ACCESS");

        assertThatThrownBy(() -> securityPolicyService.save(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullPolicyType_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("Policy");

        assertThatThrownBy(() -> securityPolicyService.save(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success_setsDefaultStatus() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("Policy");
        policy.setPolicyType("ACCESS");
        when(securityPolicyMapper.insert(any())).thenReturn(1);

        boolean result = securityPolicyService.save(policy);

        assertThat(result).isTrue();
        assertThat(policy.getStatus()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // updateById
    // ------------------------------------------------------------------

    @Test
    void updateById_nullId_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("Updated");

        assertThatThrownBy(() -> securityPolicyService.updateById(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateById_notFound_throwsNotFound() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        when(securityPolicyMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> securityPolicyService.updateById(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateById_deprecated_throwsParamError() {
        SfSecurityPolicy existing = new SfSecurityPolicy();
        existing.setId(1L);
        existing.setStatus(2);
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        policy.setName("Updated");
        when(securityPolicyMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> securityPolicyService.updateById(policy))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateById_success() {
        SfSecurityPolicy existing = new SfSecurityPolicy();
        existing.setId(1L);
        existing.setPolicyType("ACCESS");
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        policy.setName("Updated");
        policy.setPolicyType("ACCESS");
        when(securityPolicyMapper.selectById(1L)).thenReturn(existing);
        when(securityPolicyMapper.updateById(any())).thenReturn(1);

        boolean result = securityPolicyService.updateById(policy);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // activatePolicy
    // ------------------------------------------------------------------

    @Test
    void activatePolicy_notFound_throwsNotFound() {
        when(securityPolicyMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> securityPolicyService.activatePolicy(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void activatePolicy_deprecated_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        policy.setStatus(2);
        when(securityPolicyMapper.selectById(1L)).thenReturn(policy);

        assertThatThrownBy(() -> securityPolicyService.activatePolicy(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void activatePolicy_invalidRulesJson_throwsParamError() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        policy.setStatus(0);
        policy.setRulesJson("invalid json");
        when(securityPolicyMapper.selectById(1L)).thenReturn(policy);

        assertThatThrownBy(() -> securityPolicyService.activatePolicy(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void activatePolicy_success_setsActive() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        policy.setStatus(0);
        policy.setRulesJson("[{\"attribute\":\"name\",\"operator\":\"eq\",\"expectedValue\":\"test\"}]");
        when(securityPolicyMapper.selectById(1L)).thenReturn(policy);

        securityPolicyService.activatePolicy(1L);

        assertThat(policy.getStatus()).isEqualTo(1);
        verify(securityPolicyMapper).updateById(policy);
    }

    // ------------------------------------------------------------------
    // deprecatePolicy
    // ------------------------------------------------------------------

    @Test
    void deprecatePolicy_notFound_throwsNotFound() {
        when(securityPolicyMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> securityPolicyService.deprecatePolicy(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deprecatePolicy_success_setsDeprecated() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        when(securityPolicyMapper.selectById(1L)).thenReturn(policy);

        securityPolicyService.deprecatePolicy(1L);

        assertThat(policy.getStatus()).isEqualTo(2);
        verify(securityPolicyMapper).updateById(policy);
    }

    // ------------------------------------------------------------------
    // validateAgainstPolicies
    // ------------------------------------------------------------------

    @Test
    void validateAgainstPolicies_noPolicies_returnsEmpty() {
        when(securityPolicyMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<String> result = securityPolicyService.validateAgainstPolicies("ACCESS", Map.of("name", "test"));

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgainstPolicies_violation_returnsMessages() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("NameCheck");
        policy.setRulesJson("[{\"attribute\":\"name\",\"operator\":\"eq\",\"expectedValue\":\"expected\"}]");
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        List<String> result = securityPolicyService.validateAgainstPolicies("ACCESS", Map.of("name", "actual"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("Policy 'NameCheck' violated");
    }

    @Test
    void validateAgainstPolicies_compliant_returnsEmpty() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("NameCheck");
        policy.setRulesJson("[{\"attribute\":\"name\",\"operator\":\"eq\",\"expectedValue\":\"test\"}]");
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        List<String> result = securityPolicyService.validateAgainstPolicies("ACCESS", Map.of("name", "test"));

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgainstPolicies_nullRulesJson_returnsEmpty() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("NameCheck");
        policy.setRulesJson(null);
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        List<String> result = securityPolicyService.validateAgainstPolicies("ACCESS", Map.of("name", "test"));

        assertThat(result).isEmpty();
    }

    @Test
    void validateAgainstPolicies_invalidRulesJson_returnsViolation() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("NameCheck");
        policy.setRulesJson("invalid");
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        List<String> result = securityPolicyService.validateAgainstPolicies("ACCESS", Map.of("name", "test"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("invalid rules configuration");
    }

    // ------------------------------------------------------------------
    // listActivePoliciesByType
    // ------------------------------------------------------------------

    @Test
    void listActivePoliciesByType_returnsPolicies() {
        when(securityPolicyMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfSecurityPolicy> result = securityPolicyService.listActivePoliciesByType("ACCESS");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // getActivePolicyByName
    // ------------------------------------------------------------------

    @Test
    void getActivePolicyByName_found_returnsOptional() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("Policy");
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        Optional<SfSecurityPolicy> result = securityPolicyService.getActivePolicyByName("Policy");

        assertThat(result).isPresent();
    }

    @Test
    void getActivePolicyByName_notFound_returnsEmpty() {
        when(securityPolicyMapper.selectList(any())).thenReturn(Collections.emptyList());

        Optional<SfSecurityPolicy> result = securityPolicyService.getActivePolicyByName("Policy");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // isCompliant
    // ------------------------------------------------------------------

    @Test
    void isCompliant_noViolations_returnsTrue() {
        when(securityPolicyMapper.selectList(any())).thenReturn(Collections.emptyList());

        boolean result = securityPolicyService.isCompliant("ACCESS", Map.of());

        assertThat(result).isTrue();
    }

    @Test
    void isCompliant_hasViolations_returnsFalse() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setName("Check");
        policy.setRulesJson("[{\"attribute\":\"x\",\"operator\":\"eq\",\"expectedValue\":\"1\"}]");
        when(securityPolicyMapper.selectList(any())).thenReturn(List.of(policy));

        boolean result = securityPolicyService.isCompliant("ACCESS", Map.of("x", "2"));

        assertThat(result).isFalse();
    }
}
