package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.service.QualityIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/issues")
@RequiredArgsConstructor
public class QualityIssueController {

    private final QualityIssueService qualityIssueService;

    @PostMapping
    public Result<Long> create(@RequestBody SfQualityIssue qualityIssue) {
        qualityIssueService.save(qualityIssue);
        return Result.success(qualityIssue.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfQualityIssue qualityIssue) {
        qualityIssue.setId(id);
        return Result.success(qualityIssueService.updateById(qualityIssue));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(qualityIssueService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfQualityIssue> get(@PathVariable Long id) {
        SfQualityIssue issue = qualityIssueService.getById(id);
        if (issue == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(issue);
    }

    @GetMapping
    public Result<List<SfQualityIssue>> list() {
        return Result.success(qualityIssueService.list());
    }
}
