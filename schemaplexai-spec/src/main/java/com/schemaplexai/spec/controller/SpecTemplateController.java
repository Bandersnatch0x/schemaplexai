package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.service.SpecTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/templates")
@RequiredArgsConstructor
public class SpecTemplateController {

    private final SpecTemplateService specTemplateService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSpecTemplate template) {
        specTemplateService.save(template);
        return Result.success(template.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecTemplate template) {
        template.setId(id);
        return Result.success(specTemplateService.updateById(template));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specTemplateService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSpecTemplate> get(@PathVariable Long id) {
        SfSpecTemplate template = specTemplateService.getById(id);
        if (template == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(template);
    }

    @GetMapping("/page")
    public Result<PageResult<SfSpecTemplate>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecTemplate> page = specTemplateService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }
}
