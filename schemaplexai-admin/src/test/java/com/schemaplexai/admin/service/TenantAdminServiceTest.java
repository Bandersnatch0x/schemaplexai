package com.schemaplexai.admin.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.mapper.SfTenantMapper;
import com.schemaplexai.system.mapper.SfUserMapper;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAdminServiceTest {

    @Mock
    private SfTenantMapper tenantMapper;

    @Mock
    private SfUserMapper userMapper;

    @Mock
    private SfAuditLogMapper auditLogMapper;

    @InjectMocks
    private TenantAdminService tenantAdminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantAdminService, "baseMapper", tenantMapper);
    }

    // ------------------------------------------------------------------
    // getTenantAdminDetail
    // ------------------------------------------------------------------

    @Test
    void getTenantAdminDetail_notFound_throwsTenantNotFound() {
        when(tenantMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> tenantAdminService.getTenantAdminDetail(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void getTenantAdminDetail_success_returnsDto() {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        tenant.setName("Acme");
        tenant.setCode("acme");
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(auditLogMapper.selectCount(any())).thenReturn(10L);
        when(auditLogMapper.selectCount(any())).thenReturn(3L);

        var result = tenantAdminService.getTenantAdminDetail(1L);

        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getCode()).isEqualTo("acme");
    }

    // ------------------------------------------------------------------
    // listAllTenantDetails
    // ------------------------------------------------------------------

    @Test
    void listAllTenantDetails_returnsDtos() {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        tenant.setName("Acme");
        tenant.setCode("acme");
        when(tenantMapper.selectList(any())).thenReturn(List.of(tenant));
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(auditLogMapper.selectCount(any())).thenReturn(10L);

        var result = tenantAdminService.listAllTenantDetails();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Acme");
    }

    // ------------------------------------------------------------------
    // disableTenant
    // ------------------------------------------------------------------

    @Test
    void disableTenant_notFound_throwsTenantNotFound() {
        when(tenantMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> tenantAdminService.disableTenant(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void disableTenant_success_setsStatusZero() {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        tenant.setStatus(1);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        tenantAdminService.disableTenant(1L);

        assertThat(tenant.getStatus()).isEqualTo(0);
        verify(tenantMapper).updateById(tenant);
    }

    // ------------------------------------------------------------------
    // enableTenant
    // ------------------------------------------------------------------

    @Test
    void enableTenant_notFound_throwsTenantNotFound() {
        when(tenantMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> tenantAdminService.enableTenant(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void enableTenant_success_setsStatusOne() {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        tenant.setStatus(0);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        tenantAdminService.enableTenant(1L);

        assertThat(tenant.getStatus()).isEqualTo(1);
        verify(tenantMapper).updateById(tenant);
    }

    // ------------------------------------------------------------------
    // updateTenantConfig
    // ------------------------------------------------------------------

    @Test
    void updateTenantConfig_notFound_throwsTenantNotFound() {
        when(tenantMapper.selectById(1L)).thenReturn(null);

        var dto = new com.schemaplexai.admin.dto.TenantConfigUpdateDTO();
        assertThatThrownBy(() -> tenantAdminService.updateTenantConfig(1L, dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void updateTenantConfig_success_updatesConfigJson() {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        var dto = new com.schemaplexai.admin.dto.TenantConfigUpdateDTO();
        dto.setConfigJson("{\"key\":\"value\"}");

        tenantAdminService.updateTenantConfig(1L, dto);

        assertThat(tenant.getConfigJson()).isEqualTo("{\"key\":\"value\"}");
        verify(tenantMapper).updateById(tenant);
    }
}
