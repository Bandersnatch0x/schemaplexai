package com.schemaplexai.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.system.entity.*;
import com.schemaplexai.system.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    // AuthController
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

    // TenantController
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

    // UserController
    @Test
    void user_page() {
        PageParam pp = new PageParam();
        pp.setCurrent(1L); pp.setSize(20L);
        when(userService.page(any())).thenReturn(new Page<>());
        Result<?> result = userController.page(pp);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void user_getById_found() {
        SfUser u = new SfUser(); u.setId(1L);
        when(userService.getById(1L)).thenReturn(u);
        Result<SfUser> result = userController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void user_getById_notFound() {
        when(userService.getById(1L)).thenReturn(null);
        Result<SfUser> result = userController.getById(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void user_create() {
        SfUser u = new SfUser(); u.setId(1L);
        Result<Long> result = userController.create(u);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void user_update() {
        SfUser u = new SfUser();
        when(userService.updateById(any())).thenReturn(true);
        Result<Boolean> result = userController.update(1L, u);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void user_delete() {
        when(userService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = userController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // RoleController
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

    // PermissionController
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

    // ConfigController
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

    // AiModelController
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

    // ModelProviderController
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
}
