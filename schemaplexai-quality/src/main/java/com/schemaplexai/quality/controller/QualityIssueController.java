package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.service.QualityIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/issues")
@RequiredArgsConstructor
@Tag(name = "质量问题管理")
public class QualityIssueController {

    private final QualityIssueService qualityIssueService;

    @Operation(summary = "创建质量问题")
    @PostMapping
    public Result<Long> create(@RequestBody SfQualityIssue qualityIssue) {
        qualityIssueService.save(qualityIssue);
        return Result.success(qualityIssue.getId());
    }

    @Operation(summary = "更新质量问题")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfQualityIssue qualityIssue) {
        qualityIssue.setId(id);
        return Result.success(qualityIssueService.updateById(qualityIssue));
    }

    @Operation(summary = "删除质量问题")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(qualityIssueService.removeById(id));
    }

    @Operation(summary = "获取质量问题详情")
    @GetMapping("/{id}")
    public Result<SfQualityIssue> get(@PathVariable Long id) {
        SfQualityIssue issue = qualityIssueService.getById(id);
        if (issue == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(issue);
    }

    @Operation(summary = "获取质量问题列表")
    @GetMapping
    public Result<List<SfQualityIssue>> list() {
        return Result.success(qualityIssueService.list());
    }
}
