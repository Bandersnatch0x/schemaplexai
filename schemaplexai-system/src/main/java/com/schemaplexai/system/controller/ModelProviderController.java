package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfModelProvider;
import com.schemaplexai.system.service.ModelProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "模型供应商管理")
@RestController
@RequestMapping("/system/model-providers")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderService modelProviderService;

    @Operation(summary = "分页查询模型供应商")
    @GetMapping
    public Result<PageResult<SfModelProvider>> page(PageParam pageParam) {
        var page = modelProviderService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取模型供应商详情")
    @GetMapping("/{id}")
    public Result<SfModelProvider> getById(@PathVariable Long id) {
        SfModelProvider provider = modelProviderService.getById(id);
        if (provider == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(provider);
    }

    @Operation(summary = "创建模型供应商")
    @PostMapping
    public Result<Long> create(@RequestBody SfModelProvider provider) {
        modelProviderService.save(provider);
        return Result.success(provider.getId());
    }

    @Operation(summary = "更新模型供应商")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfModelProvider provider) {
        provider.setId(id);
        return Result.success(modelProviderService.updateById(provider));
    }

    @Operation(summary = "删除模型供应商")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(modelProviderService.removeById(id));
    }
}
