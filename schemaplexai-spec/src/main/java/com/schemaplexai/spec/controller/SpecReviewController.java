package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.service.SpecReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/reviews")
@RequiredArgsConstructor
@Tag(name = "规格评审管理")
public class SpecReviewController {

    private final SpecReviewService specReviewService;

    @Operation(summary = "创建规格评审")
    @PostMapping
    public Result<Long> create(@RequestBody SfSpecReview review) {
        specReviewService.save(review);
        return Result.success(review.getId());
    }

    @Operation(summary = "更新规格评审")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecReview review) {
        review.setId(id);
        return Result.success(specReviewService.updateById(review));
    }

    @Operation(summary = "删除规格评审")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specReviewService.removeById(id));
    }

    @Operation(summary = "获取规格评审详情")
    @GetMapping("/{id}")
    public Result<SfSpecReview> get(@PathVariable Long id) {
        SfSpecReview review = specReviewService.getById(id);
        if (review == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(review);
    }

    @Operation(summary = "分页查询规格评审")
    @GetMapping("/page")
    public Result<PageResult<SfSpecReview>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecReview> page = specReviewService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "提交规格评审")
    @PostMapping("/submit")
    public Result<SfSpecReview> submitReview(@RequestParam Long specId,
                                              @RequestParam Long reviewerId,
                                              @RequestParam String status,
                                              @RequestParam(required = false) String comment) {
        return Result.success(specReviewService.submitReview(specId, reviewerId, status, comment));
    }
}
