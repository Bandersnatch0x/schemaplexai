package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping
    public Result<PageResult<SfRole>> page(PageParam pageParam) {
        var page = roleService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<SfRole> getById(@PathVariable Long id) {
        SfRole role = roleService.getById(id);
        if (role == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(role);
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public Result<Long> create(@RequestBody SfRole role) {
        roleService.save(role);
        return Result.success(role.getId());
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfRole role) {
        role.setId(id);
        return Result.success(roleService.updateById(role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(roleService.removeById(id));
    }
}
