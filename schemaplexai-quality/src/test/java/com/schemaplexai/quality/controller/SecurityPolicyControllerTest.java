package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.service.SecurityPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityPolicyControllerTest {

    @Mock
    private SecurityPolicyService securityPolicyService;

    @InjectMocks
    private SecurityPolicyController securityPolicyController;

    @Test
    void create_returnsId() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);

        Result<Long> result = securityPolicyController.create(policy);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(securityPolicyService).save(policy);
    }

    @Test
    void update_returnsBoolean() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        when(securityPolicyService.updateById(any())).thenReturn(true);

        Result<Boolean> result = securityPolicyController.update(1L, policy);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(policy.getId()).isEqualTo(1L);
        verify(securityPolicyService).updateById(policy);
    }

    @Test
    void update_returnsFalse_whenServiceFails() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        when(securityPolicyService.updateById(any())).thenReturn(false);

        Result<Boolean> result = securityPolicyController.update(1L, policy);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void delete_returnsBoolean() {
        when(securityPolicyService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = securityPolicyController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(securityPolicyService).removeById(1L);
    }

    @Test
    void delete_returnsFalse_whenServiceFails() {
        when(securityPolicyService.removeById(1L)).thenReturn(false);

        Result<Boolean> result = securityPolicyController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void get_found() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        when(securityPolicyService.getById(1L)).thenReturn(policy);

        Result<SfSecurityPolicy> result = securityPolicyController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(policy);
    }

    @Test
    void get_notFound() {
        when(securityPolicyService.getById(1L)).thenReturn(null);

        Result<SfSecurityPolicy> result = securityPolicyController.get(1L);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void list_returnsPolicies() {
        SfSecurityPolicy policy = new SfSecurityPolicy();
        policy.setId(1L);
        when(securityPolicyService.list()).thenReturn(List.of(policy));

        Result<List<SfSecurityPolicy>> result = securityPolicyController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void list_returnsEmptyList() {
        when(securityPolicyService.list()).thenReturn(Collections.emptyList());

        Result<List<SfSecurityPolicy>> result = securityPolicyController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
