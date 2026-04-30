package com.schemaplexai.context.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.service.WorkspaceService;
import com.schemaplexai.model.dto.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/context/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public Result<Long> create(@RequestBody SfWorkspace workspace) {
        workspaceService.save(workspace);
        return Result.success(workspace.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkspace workspace) {
        workspace.setId(id);
        return Result.success(workspaceService.updateById(workspace));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workspaceService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfWorkspace> get(@PathVariable Long id) {
        SfWorkspace workspace = workspaceService.getById(id);
        if (workspace == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(workspace);
    }

    @GetMapping("/page")
    public Result<PageResult<SfWorkspace>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkspace> page = workspaceService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }
}
