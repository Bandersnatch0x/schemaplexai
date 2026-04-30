package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "分页查询权限")
    @GetMapping
    public Result<PageResult<SfPermission>> page(PageParam pageParam) {
        var page = permissionService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    public Result<SfPermission> getById(@PathVariable Long id) {
        SfPermission permission = permissionService.getById(id);
        if (permission == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(permission);
    }

    @Operation(summary = "创建权限")
    @PostMapping
    public Result<Long> create(@RequestBody SfPermission permission) {
        permissionService.save(permission);
        return Result.success(permission.getId());
    }

    @Operation(summary = "更新权限")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfPermission permission) {
        permission.setId(id);
        return Result.success(permissionService.updateById(permission));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(permissionService.removeById(id));
    }
}
