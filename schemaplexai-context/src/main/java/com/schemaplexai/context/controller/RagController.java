package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.service.RagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/retrieve")
    public Result<List<String>> retrieve(@RequestBody RetrieveRequest request) {
        return Result.success(ragService.retrieve(request.getQuery(), request.getTopK()));
    }

    @Data
    public static class RetrieveRequest {
        private String query;
        private int topK = 5;
    }
}
