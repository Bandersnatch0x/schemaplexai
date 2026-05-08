package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.TenantAdminDTO;
import com.schemaplexai.admin.dto.TenantConfigUpdateDTO;
import com.schemaplexai.admin.service.TenantAdminService;
import com.schemaplexai.common.result.Result;
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
class TenantAdminControllerTest {

    @Mock
    private TenantAdminService tenantAdminService;

    @InjectMocks
    private TenantAdminController tenantAdminController;

    @Test
    void listAll_returnsTenants() {
        TenantAdminDTO tenant = new TenantAdminDTO();
        tenant.setId(1L);
        when(tenantAdminService.listAllTenantDetails()).thenReturn(List.of(tenant));

        Result<List<TenantAdminDTO>> result = tenantAdminController.listAll();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void listAll_returnsEmptyList() {
        when(tenantAdminService.listAllTenantDetails()).thenReturn(Collections.emptyList());

        Result<List<TenantAdminDTO>> result = tenantAdminController.listAll();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getDetail_returnsTenant() {
        TenantAdminDTO tenant = new TenantAdminDTO();
        tenant.setId(1L);
        when(tenantAdminService.getTenantAdminDetail(1L)).thenReturn(tenant);

        Result<TenantAdminDTO> result = tenantAdminController.getDetail(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(tenant);
    }

    @Test
    void disable_returnsSuccess() {
        Result<Void> result = tenantAdminController.disable(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(tenantAdminService).disableTenant(1L);
    }

    @Test
    void enable_returnsSuccess() {
        Result<Void> result = tenantAdminController.enable(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(tenantAdminService).enableTenant(1L);
    }

    @Test
    void updateConfig_returnsSuccess() {
        TenantConfigUpdateDTO dto = new TenantConfigUpdateDTO();
        dto.setConfigJson("{}");

        Result<Void> result = tenantAdminController.updateConfig(1L, dto);

        assertThat(result.getCode()).isEqualTo(200);
        verify(tenantAdminService).updateTenantConfig(1L, dto);
    }
}
