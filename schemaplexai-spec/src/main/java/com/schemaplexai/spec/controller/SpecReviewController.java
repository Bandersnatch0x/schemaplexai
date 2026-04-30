package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.service.SpecReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/reviews")
@RequiredArgsConstructor
public class SpecReviewController {

    private final SpecReviewService specReviewService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSpecReview review) {
        specReviewService.save(review);
        return Result.success(review.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecReview review) {
        review.setId(id);
        return Result.success(specReviewService.updateById(review));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specReviewService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSpecReview> get(@PathVariable Long id) {
        SfSpecReview review = specReviewService.getById(id);
        if (review == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(review);
    }

    @GetMapping("/page")
    public Result<PageResult<SfSpecReview>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecReview> page = specReviewService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/submit")
    public Result<SfSpecReview> submitReview(@RequestParam Long specId,
                                              @RequestParam Long reviewerId,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String comment) {
        return Result.success(specReviewService.submitReview(specId, reviewerId, status, comment));
    }
}
