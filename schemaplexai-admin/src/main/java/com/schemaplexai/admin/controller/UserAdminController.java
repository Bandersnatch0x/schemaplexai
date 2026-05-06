package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.UserAdminDTO;
import com.schemaplexai.admin.dto.UserAdminQuery;
import com.schemaplexai.admin.dto.UserRoleUpdateDTO;
import com.schemaplexai.admin.service.UserAdminService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "用户管理（管理端）")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController extends BaseAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<UserAdminDTO>> page(UserAdminQuery query) {
        return success(userAdminService.queryUsers(query));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserAdminDTO> getDetail(@PathVariable Long id) {
        return success(userAdminService.getUserDetail(id));
    }

    @Operation(summary = "禁用用户")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        userAdminService.disableUser(id);
        return success();
    }

    @Operation(summary = "启用用户")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        userAdminService.enableUser(id);
        return success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPassword = userAdminService.resetUserPassword(id);
        return success(Map.of("temporaryPassword", newPassword));
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/{id}/roles")
    public Result<List<String>> getRoles(@PathVariable Long id) {
        return success(userAdminService.getUserRoles(id));
    }

    @Operation(summary = "更新用户角色")
    @PutMapping("/{id}/roles")
    public Result<Void> updateRoles(@PathVariable Long id, @RequestBody UserRoleUpdateDTO dto) {
        userAdminService.updateUserRoles(id, dto);
        return success();
    }
}
