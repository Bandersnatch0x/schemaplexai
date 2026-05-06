package com.schemaplexai.admin.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.entity.SfRolePermission;
import com.schemaplexai.system.entity.SfUserRole;
import com.schemaplexai.system.mapper.SfPermissionMapper;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
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
class RoleAdminServiceTest {

    @Mock
    private SfRoleMapper roleMapper;

    @Mock
    private SfPermissionMapper permissionMapper;

    @Mock
    private SfRolePermissionMapper rolePermissionMapper;

    @Mock
    private SfUserRoleMapper userRoleMapper;

    private RoleAdminService roleAdminService;

    @BeforeEach
    void setUp() {
        roleAdminService = spy(new RoleAdminService(permissionMapper, rolePermissionMapper, userRoleMapper));
        ReflectionTestUtils.setField(roleAdminService, "baseMapper", roleMapper);
    }

    // ------------------------------------------------------------------
    // getRoleDetail
    // ------------------------------------------------------------------

    @Test
    void getRoleDetail_notFound_throwsNotFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> roleAdminService.getRoleDetail(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getRoleDetail_success_returnsDto() {
        SfRole role = new SfRole();
        role.setId(1L);
        role.setName("ADMIN");
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        var result = roleAdminService.getRoleDetail(1L);

        assertThat(result.getName()).isEqualTo("ADMIN");
    }

    // ------------------------------------------------------------------
    // createRole
    // ------------------------------------------------------------------

    @Test
    void createRole_duplicateCode_throwsParamError() {
        SfRole existing = new SfRole();
        existing.setCode("ADMIN");
        when(roleMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> roleAdminService.createRole("tenant-1", "Admin", "ADMIN"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createRole_success_savesRole() {
        when(roleMapper.selectOne(any())).thenReturn(null);
        doReturn(true).when(roleAdminService).save(any(SfRole.class));

        var result = roleAdminService.createRole("tenant-1", "Admin", "ADMIN");

        assertThat(result.getCode()).isEqualTo("ADMIN");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        verify(roleAdminService).save(any(SfRole.class));
    }

    // ------------------------------------------------------------------
    // updateRole
    // ------------------------------------------------------------------

    @Test
    void updateRole_notFound_throwsNotFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> roleAdminService.updateRole(1L, "NewName"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateRole_success_updatesName() {
        SfRole role = new SfRole();
        role.setId(1L);
        when(roleMapper.selectById(1L)).thenReturn(role);

        roleAdminService.updateRole(1L, "NewName");

        assertThat(role.getName()).isEqualTo("NewName");
        verify(roleMapper).updateById(role);
    }

    // ------------------------------------------------------------------
    // deleteRole
    // ------------------------------------------------------------------

    @Test
    void deleteRole_notFound_throwsNotFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> roleAdminService.deleteRole(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteRole_withUsers_throwsParamError() {
        SfRole role = new SfRole();
        role.setId(1L);
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(5L);

        assertThatThrownBy(() -> roleAdminService.deleteRole(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deleteRole_success_deletesRoleAndPermissions() {
        SfRole role = new SfRole();
        role.setId(1L);
        role.setCode("ADMIN");
        when(roleMapper.selectById(1L)).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(roleAdminService).removeById(1L);

        roleAdminService.deleteRole(1L);

        verify(rolePermissionMapper).delete(any());
        verify(roleAdminService).removeById(1L);
    }

    // ------------------------------------------------------------------
    // assignPermissions
    // ------------------------------------------------------------------

    @Test
    void assignPermissions_notFound_throwsNotFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> roleAdminService.assignPermissions(1L, List.of(10L)))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void assignPermissions_success_replacesPermissions() {
        SfRole role = new SfRole();
        role.setId(1L);
        when(roleMapper.selectById(1L)).thenReturn(role);

        roleAdminService.assignPermissions(1L, List.of(10L, 20L));

        verify(rolePermissionMapper).delete(any());
        verify(rolePermissionMapper, times(2)).insert(any(SfRolePermission.class));
    }

    // ------------------------------------------------------------------
    // getRolePermissions
    // ------------------------------------------------------------------

    @Test
    void getRolePermissions_noPermissions_returnsEmpty() {
        when(rolePermissionMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<String> result = roleAdminService.getRolePermissions(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getRolePermissions_withPermissions_returnsNames() {
        SfRolePermission rp = new SfRolePermission();
        rp.setPermissionId(10L);
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rp));
        SfPermission perm = new SfPermission();
        perm.setName("READ");
        when(permissionMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(perm));

        List<String> result = roleAdminService.getRolePermissions(1L);

        assertThat(result).containsExactly("READ");
    }
}
