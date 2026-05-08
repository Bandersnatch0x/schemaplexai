package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow/templates")
@RequiredArgsConstructor
@Tag(name = "工作流模板管理", description = "工作流模板的CRUD和部署操作")
public class WorkflowTemplateController {

    private final WorkflowTemplateService workflowTemplateService;

    @Operation(summary = "创建工作流模板")
    @PostMapping
    public Result<Long> create(@RequestBody SfWorkflowTemplate template) {
        workflowTemplateService.save(template);
        return Result.success(template.getId());
    }

    @Operation(summary = "更新工作流模板")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkflowTemplate template) {
        template.setId(id);
        return Result.success(workflowTemplateService.updateById(template));
    }

    @Operation(summary = "删除工作流模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workflowTemplateService.removeById(id));
    }

    @Operation(summary = "根据ID获取工作流模板")
    @GetMapping("/{id}")
    public Result<SfWorkflowTemplate> get(@PathVariable Long id) {
        SfWorkflowTemplate template = workflowTemplateService.getById(id);
        if (template == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(template);
    }

    @Operation(summary = "分页查询工作流模板")
    @GetMapping("/page")
    public Result<PageResult<SfWorkflowTemplate>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowTemplate> page = workflowTemplateService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "部署工作流模板")
    @PostMapping("/{id}/deploy")
    public Result<SfWorkflowTemplate> deployTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.deployTemplate(id));
    }

    @Operation(summary = "验证工作流模板")
    @PostMapping("/{id}/validate")
    public Result<Boolean> validateTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.validateTemplate(id));
    }

    @Operation(summary = "克隆工作流模板")
    @PostMapping("/{id}/clone")
    public Result<SfWorkflowTemplate> cloneTemplate(@PathVariable Long id, @RequestParam String newName) {
        return Result.success(workflowTemplateService.cloneTemplate(id, newName));
    }

    @Operation(summary = "列出已部署的模板")
    @GetMapping("/deployed")
    public Result<List<SfWorkflowTemplate>> listDeployedTemplates() {
        return Result.success(workflowTemplateService.listDeployedTemplates());
    }

    @Operation(summary = "停用工作流模板")
    @PostMapping("/{id}/deactivate")
    public Result<SfWorkflowTemplate> deactivateTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.deactivateTemplate(id));
    }
}
