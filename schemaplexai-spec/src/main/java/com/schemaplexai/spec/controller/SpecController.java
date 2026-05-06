package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/spec/specs")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSpec spec) {
        specService.save(spec);
        return Result.success(spec.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpec spec) {
        spec.setId(id);
        return Result.success(specService.updateById(spec));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSpec> get(@PathVariable Long id) {
        SfSpec spec = specService.getById(id);
        if (spec == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(spec);
    }

    @GetMapping("/page")
    public Result<PageResult<SfSpec>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpec> page = specService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/{id}/publish")
    public Result<SfSpecVersion> publishSpec(@PathVariable Long id) {
        return Result.success(specService.publishSpec(id));
    }

    @PostMapping("/{id}/archive")
    public Result<Boolean> archiveSpec(@PathVariable Long id) {
        return Result.success(specService.archiveSpec(id));
    }

    @GetMapping("/{id}/latest-version")
    public Result<SfSpecVersion> getLatestVersion(@PathVariable Long id) {
        Optional<SfSpecVersion> version = specService.getLatestVersion(id);
        return version.map(Result::success).orElseGet(() -> Result.error(ResultCode.NOT_FOUND));
    }

    @GetMapping("/{id}/compare")
    public Result<List<SfSpecVersion>> compareVersions(@PathVariable Long id,
                                                        @RequestParam String versionA,
                                                        @RequestParam String versionB) {
        return Result.success(specService.compareVersions(id, versionA, versionB));
    }

    @PostMapping("/from-template")
    public Result<SfSpec> createFromTemplate(@RequestParam Long templateId,
                                              @RequestParam String title,
                                              @RequestParam String type) {
        return Result.success(specService.createFromTemplate(templateId, title, type));
    }
}
