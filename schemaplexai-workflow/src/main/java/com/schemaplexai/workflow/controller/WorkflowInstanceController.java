package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workflow/instances")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    @PostMapping
    public Result<Long> create(@RequestBody SfWorkflowInstance instance) {
        workflowInstanceService.save(instance);
        return Result.success(instance.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkflowInstance instance) {
        instance.setId(id);
        return Result.success(workflowInstanceService.updateById(instance));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workflowInstanceService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfWorkflowInstance> get(@PathVariable Long id) {
        SfWorkflowInstance instance = workflowInstanceService.getById(id);
        if (instance == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(instance);
    }

    @GetMapping("/page")
    public Result<PageResult<SfWorkflowInstance>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowInstance> page = workflowInstanceService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/{id}/trigger")
    public Result<Boolean> trigger(@PathVariable Long id) {
        workflowInstanceService.trigger(id);
        return Result.success(true);
    }
}
