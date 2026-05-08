package com.schemaplexai.context.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.service.WorkspaceService;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/workspaces")
@RequiredArgsConstructor
@Tag(name = "工作空间管理", description = "工作空间CRUD、归档与租户隔离接口")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "创建工作空间")
    public Result<Long> create(@RequestBody SfWorkspace workspace) {
        workspaceService.save(workspace);
        return Result.success(workspace.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作空间")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkspace workspace) {
        workspace.setId(id);
        return Result.success(workspaceService.updateById(workspace));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作空间")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workspaceService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取工作空间")
    public Result<SfWorkspace> get(@PathVariable Long id) {
        SfWorkspace workspace = workspaceService.getById(id);
        if (workspace == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(workspace);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询工作空间")
    public Result<PageResult<SfWorkspace>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkspace> page = workspaceService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/default")
    @Operation(summary = "创建默认工作空间")
    public Result<SfWorkspace> createDefaultWorkspace(@RequestParam String tenantId) {
        return Result.success(workspaceService.createDefaultWorkspace(tenantId));
    }

    @PostMapping("/{id}/validate-access")
    @Operation(summary = "验证工作空间访问权限")
    public Result<Void> validateWorkspaceAccess(@PathVariable Long id) {
        workspaceService.validateWorkspaceAccess(id);
        return Result.success();
    }

    @GetMapping("/by-tenant")
    @Operation(summary = "根据租户列出工作空间")
    public Result<List<SfWorkspace>> listWorkspacesByTenant(@RequestParam String tenantId) {
        return Result.success(workspaceService.listWorkspacesByTenant(tenantId));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档工作空间")
    public Result<Void> archiveWorkspace(@PathVariable Long id) {
        workspaceService.archiveWorkspace(id);
        return Result.success();
    }
}
