package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow/templates")
@RequiredArgsConstructor
public class WorkflowTemplateController {

    private final WorkflowTemplateService workflowTemplateService;

    @PostMapping
    public Result<Long> create(@RequestBody SfWorkflowTemplate template) {
        workflowTemplateService.save(template);
        return Result.success(template.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfWorkflowTemplate template) {
        template.setId(id);
        return Result.success(workflowTemplateService.updateById(template));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(workflowTemplateService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfWorkflowTemplate> get(@PathVariable Long id) {
        SfWorkflowTemplate template = workflowTemplateService.getById(id);
        if (template == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(template);
    }

    @GetMapping("/page")
    public Result<PageResult<SfWorkflowTemplate>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowTemplate> page = workflowTemplateService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/{id}/deploy")
    public Result<SfWorkflowTemplate> deployTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.deployTemplate(id));
    }

    @PostMapping("/{id}/validate")
    public Result<Boolean> validateTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.validateTemplate(id));
    }

    @PostMapping("/{id}/clone")
    public Result<SfWorkflowTemplate> cloneTemplate(@PathVariable Long id, @RequestParam String newName) {
        return Result.success(workflowTemplateService.cloneTemplate(id, newName));
    }

    @GetMapping("/deployed")
    public Result<List<SfWorkflowTemplate>> listDeployedTemplates() {
        return Result.success(workflowTemplateService.listDeployedTemplates());
    }

    @PostMapping("/{id}/deactivate")
    public Result<SfWorkflowTemplate> deactivateTemplate(@PathVariable Long id) {
        return Result.success(workflowTemplateService.deactivateTemplate(id));
    }
}
