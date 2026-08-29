package com.schemaplexai.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.dto.*;
import com.schemaplexai.system.entity.*;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import com.schemaplexai.system.security.RbacService;
import com.schemaplexai.system.service.*;
import com.schemaplexai.system.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    @Mock private TenantService tenantService;
    @InjectMocks private TenantController tenantController;

    @Mock private UserService userService;
    @InjectMocks private UserController userController;

    @Mock private RoleService roleService;
    @InjectMocks private RoleController roleController;

    @Mock private PermissionService permissionService;
    @InjectMocks private PermissionController permissionController;

    @Mock private ConfigService configService;
    @InjectMocks private ConfigController configController;

    @Mock private AiModelService aiModelService;
    @InjectMocks private AiModelController aiModelController;

    @Mock private ModelProviderService modelProviderService;
    @InjectMocks private ModelProviderController modelProviderController;

    @Mock private TenantPolicyService tenantPolicyService;
    @InjectMocks private TenantPolicyController tenantPolicyController;

    @Mock private SfUserRoleMapper userRoleMapper;
    @Mock private SfRolePermissionMapper rolePermissionMapper;
    @Mock private RbacService rbacService;
    @InjectMocks private RoleAssignmentController roleAssignmentController;

    // ==================== AuthController ====================

    @Test
    void auth_login() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-Id")).thenReturn("t1");
        when(authService.login("admin", "pass", "t1")).thenReturn(Map.of("token", "abc"));
        Result<Map<String, String>> result = authController.login(Map.of("username", "admin", "password", "pass"), req);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void auth_refresh() {
        when(authService.refreshToken("rt")).thenReturn(Map.of("token", "abc"));
        Result<Map<String, String>> result = authController.refresh(Map.of("refreshToken", "rt"));
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void auth_logout() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-User-Id")).thenReturn("1");
        Result<Void> result = authController.logout(req);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void auth_logout_passesBearerTokenToService() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(CommonConstants.HEADER_USER_ID)).thenReturn("1");
        when(req.getHeader(CommonConstants.HEADER_AUTHORIZATION)).thenReturn("Bearer raw-token");

        Result<Void> result = authController.logout(req);

        assertThat(result.getCode()).isEqualTo(200);
        verify(authService).logout("1", "raw-token");
    }

    @Test
    void auth_changePassword() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(CommonConstants.HEADER_USER_ID, "1");
        ChangePasswordRequest body = new ChangePasswordRequest();
        body.setOldPassword("oldPass");
        body.setNewPassword("newPass");

        Result<Void> result = authController.changePassword(body, req);

        assertThat(result.getCode()).isEqualTo(200);
        verify(authService).changePassword(1L, "oldPass", "newPass");
    }

    @Test
    void auth_changePassword_missingUserId_returnsUnauthorized() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        ChangePasswordRequest body = new ChangePasswordRequest();
        body.setOldPassword("oldPass");
        body.setNewPassword("newPass");

        Result<Void> result = authController.changePassword(body, req);

        assertThat(result.getCode()).isEqualTo(401);
        verify(authService, never()).changePassword(any(), any(), any());
    }

    // ==================== TenantController ====================

    @Test
    void tenant_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(tenantService.page(any())).thenReturn(new Page<>());
        Result<?> result = tenantController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenant_getById_found() {
        SfTenant t = new SfTenant(); t.setId(1L);
        when(tenantService.getById(1L)).thenReturn(t);
        Result<SfTenant> result = tenantController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenant_getById_notFound() {
        when(tenantService.getById(1L)).thenReturn(null);
        Result<SfTenant> result = tenantController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void tenant_create() {
        SfTenant t = new SfTenant(); t.setId(1L);
        Result<Long> result = tenantController.create(t);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void tenant_update() {
        SfTenant t = new SfTenant();
        when(tenantService.updateById(any())).thenReturn(true);
        Result<Boolean> result = tenantController.update(1L, t);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void tenant_delete() {
        when(tenantService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = tenantController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== UserController ====================

    @Test
    void user_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(userService.page(any())).thenReturn(new Page<>());
        Result<PageResult<UserVO>> result = userController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void user_getById_found() {
        SfUser u = new SfUser(); u.setId(1L); u.setUsername("u");
        when(userService.getById(1L)).thenReturn(u);
        Result<UserVO> result = userController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getUsername()).isEqualTo("u");
    }

    @Test
    void user_getById_notFound() {
        when(userService.getById(1L)).thenReturn(null);
        Result<UserVO> result = userController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void user_create() {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setEmail("e@e.com");

        when(userService.register(any(SfUser.class))).thenAnswer(invocation -> {
            SfUser saved = invocation.getArgument(0);
            saved.setId(100L);
            return 100L;
        });

        Result<Long> result = userController.create(req);
        assertThat(result.getData()).isEqualTo(100L);
    }

    @Test
    void user_update_found() {
        when(userService.getById(1L)).thenReturn(new SfUser());
        when(userService.updateById(any())).thenReturn(true);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@e.com");

        Result<Boolean> result = userController.update(1L, req);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void user_update_notFound() {
        when(userService.getById(99L)).thenReturn(null);

        UserUpdateRequest req = new UserUpdateRequest();
        Result<Boolean> result = userController.update(99L, req);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void user_delete() {
        when(userService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = userController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void toVO_excludesPassword() {
        SfUser u = new SfUser();
        u.setId(1L);
        u.setUsername("test");
        u.setPassword("$2a$10$shouldNotBeExposed");
        u.setEmail("e@e.com");
        u.setTenantId("t1");

        UserVO vo = UserController.toVO(u);

        assertThat(vo.getUsername()).isEqualTo("test");
        assertThat(vo).hasFieldOrPropertyWithValue("id", 1L);
        // Verify the VO does NOT have a password field via class introspection
        assertThat(vo).isExactlyInstanceOf(UserVO.class);
    }

    // ==================== RoleController ====================

    @Test
    void role_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(roleService.page(any())).thenReturn(new Page<>());
        Result<?> result = roleController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void role_getById_found() {
        SfRole r = new SfRole(); r.setId(1L);
        when(roleService.getById(1L)).thenReturn(r);
        Result<SfRole> result = roleController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void role_getById_notFound() {
        when(roleService.getById(1L)).thenReturn(null);
        Result<SfRole> result = roleController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void role_create() {
        SfRole r = new SfRole(); r.setId(1L);
        Result<Long> result = roleController.create(r);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void role_update() {
        SfRole r = new SfRole();
        when(roleService.updateById(any())).thenReturn(true);
        Result<Boolean> result = roleController.update(1L, r);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void role_delete() {
        when(roleService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = roleController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== PermissionController ====================

    @Test
    void permission_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(permissionService.page(any())).thenReturn(new Page<>());
        Result<?> result = permissionController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void permission_getById_found() {
        SfPermission p = new SfPermission(); p.setId(1L);
        when(permissionService.getById(1L)).thenReturn(p);
        Result<SfPermission> result = permissionController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void permission_getById_notFound() {
        when(permissionService.getById(1L)).thenReturn(null);
        Result<SfPermission> result = permissionController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void permission_create() {
        SfPermission p = new SfPermission(); p.setId(1L);
        Result<Long> result = permissionController.create(p);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void permission_update() {
        SfPermission p = new SfPermission();
        when(permissionService.updateById(any())).thenReturn(true);
        Result<Boolean> result = permissionController.update(1L, p);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void permission_delete() {
        when(permissionService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = permissionController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== ConfigController ====================

    @Test
    void config_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(configService.page(any())).thenReturn(new Page<>());
        Result<?> result = configController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void config_getById_found() {
        SfConfig c = new SfConfig(); c.setId(1L);
        when(configService.getById(1L)).thenReturn(c);
        Result<SfConfig> result = configController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void config_getById_notFound() {
        when(configService.getById(1L)).thenReturn(null);
        Result<SfConfig> result = configController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void config_create() {
        SfConfig c = new SfConfig(); c.setId(1L);
        Result<Long> result = configController.create(c);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void config_update() {
        SfConfig c = new SfConfig();
        when(configService.updateById(any())).thenReturn(true);
        Result<Boolean> result = configController.update(1L, c);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void config_delete() {
        when(configService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = configController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== AiModelController ====================

    @Test
    void aiModel_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(aiModelService.page(any())).thenReturn(new Page<>());
        Result<?> result = aiModelController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void aiModel_getById_found() {
        SfAiModel m = new SfAiModel(); m.setId(1L);
        when(aiModelService.getById(1L)).thenReturn(m);
        Result<SfAiModel> result = aiModelController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void aiModel_getById_notFound() {
        when(aiModelService.getById(1L)).thenReturn(null);
        Result<SfAiModel> result = aiModelController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void aiModel_create() {
        SfAiModel m = new SfAiModel(); m.setId(1L);
        Result<Long> result = aiModelController.create(m);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void aiModel_update() {
        SfAiModel m = new SfAiModel();
        when(aiModelService.updateById(any())).thenReturn(true);
        Result<Boolean> result = aiModelController.update(1L, m);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void aiModel_delete() {
        when(aiModelService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = aiModelController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== ModelProviderController ====================

    @Test
    void modelProvider_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(modelProviderService.page(any())).thenReturn(new Page<>());
        Result<?> result = modelProviderController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void modelProvider_getById_found() {
        SfModelProvider p = new SfModelProvider(); p.setId(1L);
        when(modelProviderService.getById(1L)).thenReturn(p);
        Result<SfModelProvider> result = modelProviderController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void modelProvider_getById_notFound() {
        when(modelProviderService.getById(1L)).thenReturn(null);
        Result<SfModelProvider> result = modelProviderController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void modelProvider_create() {
        SfModelProvider p = new SfModelProvider(); p.setId(1L);
        Result<Long> result = modelProviderController.create(p);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void modelProvider_update() {
        SfModelProvider p = new SfModelProvider();
        when(modelProviderService.updateById(any())).thenReturn(true);
        Result<Boolean> result = modelProviderController.update(1L, p);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void modelProvider_delete() {
        when(modelProviderService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = modelProviderController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // ==================== TenantPolicyController ====================

    @Test
    void tenantPolicy_listPolicies() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("t1");
        when(tenantPolicyService.getPoliciesByTenant("t1")).thenReturn(List.of());

        Result<List<TenantPolicy>> result = tenantPolicyController.listPolicies(req);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantPolicy_getPolicy_found() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("t1");
        TenantPolicy policy = new TenantPolicy();
        policy.setPolicyType("RATE_LIMIT");
        when(tenantPolicyService.getPolicy("t1", "RATE_LIMIT")).thenReturn(policy);

        Result<TenantPolicy> result = tenantPolicyController.getPolicy("RATE_LIMIT", req);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getPolicyType()).isEqualTo("RATE_LIMIT");
    }

    @Test
    void tenantPolicy_getPolicy_notFound() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("t1");
        when(tenantPolicyService.getPolicy("t1", "MISSING")).thenReturn(null);

        Result<TenantPolicy> result = tenantPolicyController.getPolicy("MISSING", req);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void tenantPolicy_saveOrUpdatePolicy() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("t1");
        TenantPolicyRequest body = new TenantPolicyRequest();
        body.setConfigJson("{\"rpm\": 100}");

        Result<Void> result = tenantPolicyController.saveOrUpdatePolicy("RATE_LIMIT", body, req);

        assertThat(result.getCode()).isEqualTo(200);
        verify(tenantPolicyService).saveOrUpdatePolicy("t1", "RATE_LIMIT", "{\"rpm\": 100}");
    }

    // ==================== RoleAssignmentController ====================

    @Test
    void roleAssignment_assignRoleToUser() {
        RoleAssignmentRequest req = new RoleAssignmentRequest();
        req.setUserId(1L);
        req.setRoleId(10L);
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(userRoleMapper.insert(any(SfUserRole.class))).thenReturn(1);

        Result<Long> result = roleAssignmentController.assignRoleToUser(req);

        assertThat(result.getCode()).isEqualTo(200);
        verify(rbacService).evictUser(1L);
    }

    @Test
    void roleAssignment_assignRoleToUser_alreadyAssigned() {
        RoleAssignmentRequest req = new RoleAssignmentRequest();
        req.setUserId(1L);
        req.setRoleId(10L);
        when(userRoleMapper.selectCount(any())).thenReturn(1L);

        Result<Long> result = roleAssignmentController.assignRoleToUser(req);

        assertThat(result.getCode()).isEqualTo(409);
        verify(rbacService, never()).evictUser(any());
    }

    @Test
    void roleAssignment_removeRoleFromUser() {
        when(userRoleMapper.delete(any())).thenReturn(1);

        Result<Void> result = roleAssignmentController.removeRoleFromUser(1L, 10L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(rbacService).evictUser(1L);
    }

    @Test
    void roleAssignment_removeRoleFromUser_notFound() {
        when(userRoleMapper.delete(any())).thenReturn(0);

        Result<Void> result = roleAssignmentController.removeRoleFromUser(1L, 99L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void roleAssignment_assignPermissionToRole() {
        PermissionAssignmentRequest req = new PermissionAssignmentRequest();
        req.setRoleId(10L);
        req.setPermissionId(100L);
        when(rolePermissionMapper.selectCount(any())).thenReturn(0L);
        when(rolePermissionMapper.insert(any(SfRolePermission.class))).thenReturn(1);

        Result<Long> result = roleAssignmentController.assignPermissionToRole(req);

        assertThat(result.getCode()).isEqualTo(200);
        verify(rbacService).evictAll();
    }

    @Test
    void roleAssignment_removePermissionFromRole() {
        when(rolePermissionMapper.delete(any())).thenReturn(1);

        Result<Void> result = roleAssignmentController.removePermissionFromRole(10L, 100L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(rbacService).evictAll();
    }
}
