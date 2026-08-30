package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/workflow/instances")
@RequiredArgsConstructor
@Tag(name = "工作流实例管理", description = "工作流实例的CRUD和触发操作")
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    @Operation(summary = "创建工作流实例")
    @PostMapping
    public Result<Long> create(@RequestBody SfWorkflowInstance instance) {
        workflowInstanceService.save(instance);
        return Result.success(instance.getId());
    }

    @Operation(summary = "更新工作流实例")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkflowInstance instance) {
        instance.setId(id);
        return Result.success(workflowInstanceService.updateById(instance));
    }

    @Operation(summary = "删除工作流实例")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workflowInstanceService.removeById(id));
    }

    @Operation(summary = "根据ID获取工作流实例")
    @GetMapping("/{id}")
    public Result<SfWorkflowInstance> get(@PathVariable Long id) {
        SfWorkflowInstance instance = workflowInstanceService.getById(id);
        if (instance == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(instance);
    }

    @Operation(summary = "分页查询工作流实例")
    @GetMapping("/page")
    public Result<PageResult<SfWorkflowInstance>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowInstance> page = workflowInstanceService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "触发工作流实例")
    @PostMapping("/{id}/trigger")
    public Result<Boolean> trigger(@PathVariable Long id) {
        workflowInstanceService.trigger(id);
        return Result.success(true);
    }

    @Operation(summary = "取消工作流实例", description = "仅 PENDING / RUNNING / WAITING_APPROVAL 状态可取消（规格 §6.2）")
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        workflowInstanceService.cancel(id);
        return Result.success(true);
    }

    @Operation(summary = "人工审批通过", description = "恢复等待审批的实例并继续执行（规格 §6.2）")
    @PostMapping("/{id}/approve")
    public Result<Boolean> approve(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, String> body) {
        workflowInstanceService.approve(id, body != null ? body.get("comment") : null);
        return Result.success(true);
    }

    @Operation(summary = "人工审批拒绝", description = "拒绝等待审批的实例，实例进入 FAILED（规格 §6.2）")
    @PostMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body) {
        workflowInstanceService.reject(id, body != null ? body.get("reason") : null);
        return Result.success(true);
    }
}
