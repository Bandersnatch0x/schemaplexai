package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.RoleAdminDTO;
import com.schemaplexai.admin.service.RoleAdminService;
import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAdminControllerTest {

    @Mock
    private RoleAdminService roleAdminService;

    @InjectMocks
    private RoleAdminController roleAdminController;

    @Test
    void listAll_returnsRoles() {
        RoleAdminDTO role = new RoleAdminDTO();
        role.setId(1L);
        when(roleAdminService.listAllRoles(null)).thenReturn(List.of(role));

        Result<List<RoleAdminDTO>> result = roleAdminController.listAll(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void listAll_withTenantId() {
        RoleAdminDTO role = new RoleAdminDTO();
        role.setId(1L);
        when(roleAdminService.listAllRoles("tenant1")).thenReturn(List.of(role));

        Result<List<RoleAdminDTO>> result = roleAdminController.listAll("tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void getDetail_returnsRole() {
        RoleAdminDTO role = new RoleAdminDTO();
        role.setId(1L);
        when(roleAdminService.getRoleDetail(1L)).thenReturn(role);

        Result<RoleAdminDTO> result = roleAdminController.getDetail(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(role);
    }

    @Test
    void create_returnsRole() {
        RoleAdminDTO role = new RoleAdminDTO();
        role.setId(1L);
        when(roleAdminService.createRole("tenant1", "Admin", "admin")).thenReturn(role);

        Result<RoleAdminDTO> result = roleAdminController.create(Map.of("tenantId", "tenant1", "name", "Admin", "code", "admin"));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(role);
    }

    @Test
    void update_returnsSuccess() {
        Result<Void> result = roleAdminController.update(1L, Map.of("name", "SuperAdmin"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(roleAdminService).updateRole(1L, "SuperAdmin");
    }

    @Test
    void delete_returnsSuccess() {
        Result<Void> result = roleAdminController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(roleAdminService).deleteRole(1L);
    }

    @Test
    void assignPermissions_returnsSuccess() {
        Result<Void> result = roleAdminController.assignPermissions(1L, Map.of("permissionIds", List.of(1L, 2L)));

        assertThat(result.getCode()).isEqualTo(200);
        verify(roleAdminService).assignPermissions(1L, List.of(1L, 2L));
    }

    @Test
    void getPermissions_returnsPermissionNames() {
        when(roleAdminService.getRolePermissions(1L)).thenReturn(List.of("READ", "WRITE"));

        Result<List<String>> result = roleAdminController.getPermissions(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly("READ", "WRITE");
    }
}
