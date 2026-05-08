package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/rag")
@RequiredArgsConstructor
@Tag(name = "RAG检索", description = "基于向量检索的知识问答接口")
public class RagController {

    private final RagService ragService;

    @PostMapping("/retrieve")
    @Operation(summary = "检索相关知识片段")
    public Result<List<String>> retrieve(@RequestBody RetrieveRequest request) {
        return Result.success(ragService.retrieve(request.getQuery(), request.getTopK()));
    }

    @Data
    public static class RetrieveRequest {
        private String query;
        private int topK = 5;
    }
}
