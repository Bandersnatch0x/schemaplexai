package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.RoleAdminDTO;
import com.schemaplexai.admin.service.RoleAdminService;
import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "角色管理（管理端）")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController extends BaseAdminController {

    private final RoleAdminService roleAdminService;

    @Operation(summary = "查询所有角色")
    @GetMapping
    public Result<List<RoleAdminDTO>> listAll(@RequestParam(required = false) String tenantId) {
        return success(roleAdminService.listAllRoles(tenantId));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<RoleAdminDTO> getDetail(@PathVariable Long id) {
        return success(roleAdminService.getRoleDetail(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public Result<RoleAdminDTO> create(@RequestBody Map<String, String> request) {
        String tenantId = request.get("tenantId");
        String name = request.get("name");
        String code = request.get("code");
        return success(roleAdminService.createRole(tenantId, name, code));
    }

    @Operation(summary = "更新角色名称")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, String> request) {
        roleAdminService.updateRole(id, request.get("name"));
        return success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleAdminService.deleteRole(id);
        return success();
    }

    @Operation(summary = "分配权限")
    @PostMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        roleAdminService.assignPermissions(id, request.get("permissionIds"));
        return success();
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    public Result<List<String>> getPermissions(@PathVariable Long id) {
        return success(roleAdminService.getRolePermissions(id));
    }
}
