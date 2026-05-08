package com.schemaplexai.context.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.service.KnowledgeDocService;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/context/knowledge-docs")
@RequiredArgsConstructor
@Tag(name = "知识文档管理", description = "知识文档上传、向量化与分页查询接口")
public class KnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;

    @PostMapping
    @Operation(summary = "上传并向量知识文档")
    public Result<Long> create(@RequestBody SfKnowledgeDoc doc) {
        knowledgeDocService.uploadAndVectorize(doc);
        return Result.success(doc.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识文档")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfKnowledgeDoc doc) {
        doc.setId(id);
        return Result.success(knowledgeDocService.updateById(doc));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识文档")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(knowledgeDocService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取知识文档")
    public Result<SfKnowledgeDoc> get(@PathVariable Long id) {
        SfKnowledgeDoc doc = knowledgeDocService.getById(id);
        if (doc == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(doc);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询知识文档")
    public Result<PageResult<SfKnowledgeDoc>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfKnowledgeDoc> page = knowledgeDocService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }
}
