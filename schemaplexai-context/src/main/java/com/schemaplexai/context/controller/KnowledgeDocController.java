package com.schemaplexai.context.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.service.KnowledgeDocService;
import com.schemaplexai.model.dto.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/context/knowledge-docs")
@RequiredArgsConstructor
public class KnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;

    @PostMapping
    public Result<Long> create(@RequestBody SfKnowledgeDoc doc) {
        knowledgeDocService.uploadAndVectorize(doc);
        return Result.success(doc.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfKnowledgeDoc doc) {
        doc.setId(id);
        return Result.success(knowledgeDocService.updateById(doc));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(knowledgeDocService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfKnowledgeDoc> get(@PathVariable Long id) {
        SfKnowledgeDoc doc = knowledgeDocService.getById(id);
        if (doc == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(doc);
    }

    @GetMapping("/page")
    public Result<PageResult<SfKnowledgeDoc>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfKnowledgeDoc> page = knowledgeDocService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }
}
