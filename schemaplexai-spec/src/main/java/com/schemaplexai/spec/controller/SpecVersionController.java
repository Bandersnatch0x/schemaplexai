package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/versions")
@RequiredArgsConstructor
public class SpecVersionController {

    private final SpecVersionService specVersionService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSpecVersion version) {
        specVersionService.save(version);
        return Result.success(version.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecVersion version) {
        version.setId(id);
        return Result.success(specVersionService.updateById(version));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specVersionService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSpecVersion> get(@PathVariable Long id) {
        SfSpecVersion version = specVersionService.getById(id);
        if (version == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(version);
    }

    @GetMapping("/page")
    public Result<PageResult<SfSpecVersion>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecVersion> page = specVersionService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @GetMapping("/diff")
    public Result<SpecDiffResult> diff(@RequestParam Long versionAId, @RequestParam Long versionBId) {
        return Result.success(specVersionService.diff(versionAId, versionBId));
    }

    @PostMapping("/publish")
    public Result<SfSpecVersion> publish(@RequestParam Long specId,
                                          @RequestParam String version,
                                          @RequestParam String content,
                                          @RequestParam(required = false) String changeLog) {
        return Result.success(specVersionService.createVersion(specId, version, content, changeLog));
    }
}
