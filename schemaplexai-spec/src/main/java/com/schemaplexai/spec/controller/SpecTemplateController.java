package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.service.SpecTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/spec/templates")
@RequiredArgsConstructor
@Tag(name = "规格模板管理")
public class SpecTemplateController {

    private final SpecTemplateService specTemplateService;

    @Operation(summary = "创建规格模板")
    @PostMapping
    public Result<Long> create(@RequestBody SfSpecTemplate template) {
        specTemplateService.save(template);
        return Result.success(template.getId());
    }

    @Operation(summary = "更新规格模板")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecTemplate template) {
        template.setId(id);
        return Result.success(specTemplateService.updateById(template));
    }

    @Operation(summary = "删除规格模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specTemplateService.removeById(id));
    }

    @Operation(summary = "获取规格模板详情")
    @GetMapping("/{id}")
    public Result<SfSpecTemplate> get(@PathVariable Long id) {
        SfSpecTemplate template = specTemplateService.getById(id);
        if (template == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(template);
    }

    @Operation(summary = "分页查询规格模板")
    @GetMapping("/page")
    public Result<PageResult<SfSpecTemplate>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecTemplate> page = specTemplateService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "应用规格模板")
    @PostMapping("/{id}/apply")
    public Result<SfSpec> applyTemplate(@PathVariable Long id,
                                         @RequestParam(required = false) Long specId,
                                         @RequestParam String title,
                                         @RequestParam String type) {
        return Result.success(specTemplateService.applyTemplate(id, specId, title, type));
    }

    @Operation(summary = "获取默认模板")
    @GetMapping("/default")
    public Result<SfSpecTemplate> getDefaultTemplate(@RequestParam String category) {
        Optional<SfSpecTemplate> template = specTemplateService.getDefaultTemplate(category);
        return template.map(Result::success).orElseGet(() -> Result.error(ResultCode.NOT_FOUND));
    }

    @Operation(summary = "按分类查询模板")
    @GetMapping("/by-category")
    public Result<List<SfSpecTemplate>> listTemplatesByCategory(@RequestParam String category) {
        return Result.success(specTemplateService.listTemplatesByCategory(category));
    }

    @Operation(summary = "克隆规格模板")
    @PostMapping("/{id}/clone")
    public Result<SfSpecTemplate> cloneTemplate(@PathVariable Long id, @RequestParam String newName) {
        return Result.success(specTemplateService.cloneTemplate(id, newName));
    }
}
