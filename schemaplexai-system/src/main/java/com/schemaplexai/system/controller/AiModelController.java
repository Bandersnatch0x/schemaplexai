package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfAiModel;
import com.schemaplexai.system.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI模型管理")
@RestController
@RequestMapping("/system/models")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService aiModelService;

    @Operation(summary = "分页查询AI模型")
    @GetMapping
    public Result<PageResult<SfAiModel>> page(PageParam pageParam) {
        var page = aiModelService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取AI模型详情")
    @GetMapping("/{id}")
    public Result<SfAiModel> getById(@PathVariable Long id) {
        SfAiModel model = aiModelService.getById(id);
        if (model == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(model);
    }

    @Operation(summary = "创建AI模型")
    @PostMapping
    public Result<Long> create(@RequestBody SfAiModel model) {
        aiModelService.save(model);
        return Result.success(model.getId());
    }

    @Operation(summary = "更新AI模型")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfAiModel model) {
        model.setId(id);
        return Result.success(aiModelService.updateById(model));
    }

    @Operation(summary = "删除AI模型")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(aiModelService.removeById(id));
    }
}
