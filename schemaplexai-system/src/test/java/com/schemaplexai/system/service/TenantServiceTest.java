package com.schemaplexai.system.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.mapper.SfTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private SfTenantMapper tenantMapper;

    private TenantService tenantService;

    private SfTenant sampleTenant;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService();
        // ServiceImpl stores the mapper in the baseMapper field
        ReflectionTestUtils.setField(tenantService, "baseMapper", tenantMapper);

        sampleTenant = new SfTenant();
        sampleTenant.setId(1L);
        sampleTenant.setName("Test Tenant");
        sampleTenant.setCode("TEST");
        sampleTenant.setStatus(1);
        sampleTenant.setConfigJson("{}");
    }

    @Test
    void getValidTenant_existingActiveTenant_returnsTenant() {
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        SfTenant result = tenantService.getValidTenant(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        assertThat(result.getCode()).isEqualTo("TEST");
        assertThat(result.getStatus()).isEqualTo(1);
    }

    @Test
    void getValidTenant_nonExistentTenant_throwsTenantNotFound() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> tenantService.getValidTenant(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void getValidTenant_disabledTenant_throwsTenantDisabled() {
        sampleTenant.setStatus(0);
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        assertThatThrownBy(() -> tenantService.getValidTenant(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_DISABLED.getCode());
    }

    @Test
    void getValidTenant_nullStatus_doesNotThrow() {
        sampleTenant.setStatus(null);
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        SfTenant result = tenantService.getValidTenant(1L);

        assertThat(result).isNotNull();
    }
}
