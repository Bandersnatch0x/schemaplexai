package com.schemaplexai.context.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.service.ContextService;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/contexts")
@RequiredArgsConstructor
@Tag(name = "上下文管理", description = "上下文创建、查询、刷新与搜索接口")
public class ContextController {

    private final ContextService contextService;

    @PostMapping
    @Operation(summary = "创建上下文")
    public Result<Long> create(@RequestBody SfContext context) {
        contextService.save(context);
        return Result.success(context.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新上下文")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfContext context) {
        context.setId(id);
        return Result.success(contextService.updateById(context));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除上下文")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(contextService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取上下文")
    public Result<SfContext> get(@PathVariable Long id) {
        SfContext context = contextService.getById(id);
        if (context == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(context);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询上下文")
    public Result<PageResult<SfContext>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfContext> page = contextService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @PostMapping("/ingest")
    @Operation(summary = "摄入上下文")
    public Result<SfContext> ingestContext(@RequestParam Long workspaceId,
                                           @RequestParam String name,
                                           @RequestParam String type) {
        return Result.success(contextService.ingestContext(workspaceId, name, type));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索上下文")
    public Result<List<SfContext>> searchContext(@RequestParam String keyword,
                                                  @RequestParam(required = false) String type) {
        return Result.success(contextService.searchContext(keyword, type));
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "刷新上下文")
    public Result<Void> refreshContext(@PathVariable Long id) {
        contextService.refreshContext(id);
        return Result.success();
    }

    @GetMapping("/by-conversation")
    @Operation(summary = "根据会话ID获取上下文")
    public Result<SfContext> getContextByConversation(@RequestParam Long conversationId) {
        SfContext context = contextService.getContextByConversation(conversationId);
        if (context == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(context);
    }
}
