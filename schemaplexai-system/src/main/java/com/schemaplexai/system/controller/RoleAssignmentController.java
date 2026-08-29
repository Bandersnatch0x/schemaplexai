package com.schemaplexai.system.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.dto.PermissionAssignmentRequest;
import com.schemaplexai.system.dto.RoleAssignmentRequest;
import com.schemaplexai.system.entity.SfRolePermission;
import com.schemaplexai.system.entity.SfUserRole;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import com.schemaplexai.system.security.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing role-to-user and permission-to-role assignments.
 */
@Tag(name = "角色权限分配")
@RestController
@RequestMapping("/system/role-assignments")
@RequiredArgsConstructor
public class RoleAssignmentController {

    private final SfUserRoleMapper userRoleMapper;
    private final SfRolePermissionMapper rolePermissionMapper;
    private final RbacService rbacService;

    // -------- User-Role assignments --------

    @Operation(summary = "为用户分配角色")
    @PostMapping
    @Transactional
    public Result<Long> assignRoleToUser(@Valid @RequestBody RoleAssignmentRequest request) {
        // Check if assignment already exists
        long count = userRoleMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUserRole>()
                        .eq(SfUserRole::getUserId, request.getUserId())
                        .eq(SfUserRole::getRoleId, request.getRoleId())
        );
        if (count > 0) {
            return Result.error(ResultCode.CONFLICT.getCode(), "Role already assigned to user");
        }

        SfUserRole userRole = new SfUserRole();
        userRole.setUserId(request.getUserId());
        userRole.setRoleId(request.getRoleId());
        userRoleMapper.insert(userRole);

        rbacService.evictUser(request.getUserId());
        return Result.success(userRole.getId());
    }

    @Operation(summary = "移除用户的角色")
    @DeleteMapping("/{userId}/{roleId}")
    @Transactional
    public Result<Void> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        int deleted = userRoleMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUserRole>()
                        .eq(SfUserRole::getUserId, userId)
                        .eq(SfUserRole::getRoleId, roleId)
        );
        if (deleted == 0) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Role assignment not found");
        }

        rbacService.evictUser(userId);
        return Result.success();
    }

    // -------- Role-Permission assignments --------

    @Operation(summary = "为角色分配权限")
    @PostMapping("/permissions")
    @Transactional
    public Result<Long> assignPermissionToRole(@Valid @RequestBody PermissionAssignmentRequest request) {
        long count = rolePermissionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfRolePermission>()
                        .eq(SfRolePermission::getRoleId, request.getRoleId())
                        .eq(SfRolePermission::getPermissionId, request.getPermissionId())
        );
        if (count > 0) {
            return Result.error(ResultCode.CONFLICT.getCode(), "Permission already assigned to role");
        }

        SfRolePermission rolePermission = new SfRolePermission();
        rolePermission.setRoleId(request.getRoleId());
        rolePermission.setPermissionId(request.getPermissionId());
        rolePermissionMapper.insert(rolePermission);

        // Evict cache for all users who have this role — conservative approach.
        // In a high-traffic system this would be optimized with event-driven cache invalidation.
        rbacService.evictAll();
        return Result.success(rolePermission.getId());
    }

    @Operation(summary = "移除角色的权限")
    @DeleteMapping("/permissions/{roleId}/{permissionId}")
    @Transactional
    public Result<Void> removePermissionFromRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        int deleted = rolePermissionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfRolePermission>()
                        .eq(SfRolePermission::getRoleId, roleId)
                        .eq(SfRolePermission::getPermissionId, permissionId)
        );
        if (deleted == 0) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Permission assignment not found");
        }

        rbacService.evictAll();
        return Result.success();
    }
}
