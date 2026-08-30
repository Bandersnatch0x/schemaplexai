package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/spec/specs")
@RequiredArgsConstructor
@Tag(name = "需求规格管理")
public class SpecController {

    private final SpecService specService;

    @Operation(summary = "创建需求规格")
    @PostMapping
    @PreAuthorize("hasAuthority('spec:write')")
    public Result<Long> create(@RequestBody SfSpec spec) {
        return Result.success(specService.createSpec(spec).getId());
    }

    @Operation(summary = "更新需求规格")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('spec:write')")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpec spec) {
        return Result.success(specService.updateSpec(id, spec));
    }

    @Operation(summary = "删除需求规格")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('spec:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specService.deleteSpec(id));
    }

    @Operation(summary = "获取需求规格详情")
    @GetMapping("/{id}")
    public Result<SfSpec> get(@PathVariable Long id) {
        SfSpec spec = specService.getById(id);
        if (spec == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(spec);
    }

    @Operation(summary = "分页查询需求规格")
    @GetMapping("/page")
    public Result<PageResult<SfSpec>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpec> page = specService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "发布需求规格")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('spec:publish')")
    public Result<SfSpecVersion> publishSpec(@PathVariable Long id) {
        return Result.success(specService.publishSpec(id));
    }

    @Operation(summary = "归档需求规格")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('spec:delete')")
    public Result<Boolean> archiveSpec(@PathVariable Long id) {
        return Result.success(specService.archiveSpec(id));
    }

    @Operation(summary = "回滚到指定历史版本")
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasAuthority('spec:rollback')")
    public Result<SfSpec> rollbackSpec(@PathVariable Long id, @RequestParam Long versionId) {
        return Result.success(specService.rollbackSpec(id, versionId));
    }

    @Operation(summary = "获取最新版本")
    @GetMapping("/{id}/latest-version")
    public Result<SfSpecVersion> getLatestVersion(@PathVariable Long id) {
        Optional<SfSpecVersion> version = specService.getLatestVersion(id);
        return version.map(Result::success).orElseGet(() -> Result.error(ResultCode.NOT_FOUND));
    }

    @Operation(summary = "对比版本差异")
    @GetMapping("/{id}/compare")
    public Result<List<SfSpecVersion>> compareVersions(@PathVariable Long id,
                                                        @RequestParam String versionA,
                                                        @RequestParam String versionB) {
        return Result.success(specService.compareVersions(id, versionA, versionB));
    }

    @Operation(summary = "从模板创建需求规格")
    @PostMapping("/from-template")
    @PreAuthorize("hasAuthority('spec:write')")
    public Result<SfSpec> createFromTemplate(@RequestParam Long templateId,
                                              @RequestParam String title,
                                              @RequestParam String type) {
        return Result.success(specService.createFromTemplate(templateId, title, type));
    }
}
