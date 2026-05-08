package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/reviews")
@RequiredArgsConstructor
@Tag(name = "质量评审管理")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "创建评审记录")
    @PostMapping
    public Result<Long> create(@RequestBody SfReviewRecord reviewRecord) {
        reviewService.save(reviewRecord);
        return Result.success(reviewRecord.getId());
    }

    @Operation(summary = "更新评审记录")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfReviewRecord reviewRecord) {
        reviewRecord.setId(id);
        return Result.success(reviewService.updateById(reviewRecord));
    }

    @Operation(summary = "删除评审记录")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reviewService.removeById(id));
    }

    @Operation(summary = "获取评审记录详情")
    @GetMapping("/{id}")
    public Result<SfReviewRecord> get(@PathVariable Long id) {
        SfReviewRecord record = reviewService.getById(id);
        if (record == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(record);
    }

    @Operation(summary = "获取评审记录列表")
    @GetMapping
    public Result<List<SfReviewRecord>> list() {
        return Result.success(reviewService.list());
    }
}
