package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<Long> create(@RequestBody SfReviewRecord reviewRecord) {
        reviewService.save(reviewRecord);
        return Result.success(reviewRecord.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfReviewRecord reviewRecord) {
        reviewRecord.setId(id);
        return Result.success(reviewService.updateById(reviewRecord));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reviewService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfReviewRecord> get(@PathVariable Long id) {
        SfReviewRecord record = reviewService.getById(id);
        if (record == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(record);
    }

    @GetMapping
    public Result<List<SfReviewRecord>> list() {
        return Result.success(reviewService.list());
    }
}
