package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/versions")
@RequiredArgsConstructor
@Tag(name = "规格版本管理")
public class SpecVersionController {

    private final SpecVersionService specVersionService;

    @Operation(summary = "创建规格版本")
    @PostMapping
    public Result<Long> create(@RequestBody SfSpecVersion version) {
        specVersionService.save(version);
        return Result.success(version.getId());
    }

    @Operation(summary = "更新规格版本")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecVersion version) {
        version.setId(id);
        return Result.success(specVersionService.updateById(version));
    }

    @Operation(summary = "删除规格版本")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specVersionService.removeById(id));
    }

    @Operation(summary = "获取规格版本详情")
    @GetMapping("/{id}")
    public Result<SfSpecVersion> get(@PathVariable Long id) {
        SfSpecVersion version = specVersionService.getById(id);
        if (version == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(version);
    }

    @Operation(summary = "分页查询规格版本")
    @GetMapping("/page")
    public Result<PageResult<SfSpecVersion>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecVersion> page = specVersionService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "对比版本差异")
    @GetMapping("/diff")
    public Result<SpecDiffResult> diff(@RequestParam Long versionAId, @RequestParam Long versionBId) {
        return Result.success(specVersionService.diff(versionAId, versionBId));
    }

    @Operation(summary = "发布规格版本")
    @PostMapping("/publish")
    public Result<SfSpecVersion> publish(@RequestParam Long specId,
                                          @RequestParam String version,
                                          @RequestParam String content,
                                          @RequestParam(required = false) String changeLog) {
        return Result.success(specVersionService.createVersion(specId, version, content, changeLog));
    }
}
