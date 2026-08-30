package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.dto.RagSearchItem;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.service.RagSearchService;
import com.schemaplexai.context.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/rag")
@RequiredArgsConstructor
@Tag(name = "RAG检索", description = "基于向量检索的知识问答接口")
public class RagController {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 50;

    private final RagService ragService;
    /** Optional: the vector search bean exists only when milvus.enabled=true. */
    private final ObjectProvider<RagSearchService> ragSearchServiceProvider;

    @PostMapping("/retrieve")
    @Operation(summary = "检索相关知识片段")
    public Result<List<String>> retrieve(@RequestBody RetrieveRequest request) {
        return Result.success(ragService.retrieve(request.getQuery(), request.getTopK()));
    }

    /**
     * Spec §4.1 vector search endpoint. Request: query / topK / filters.docType.
     * Response items: docId, docName, content, score, metadata.
     */
    @PostMapping("/search")
    @Operation(summary = "向量检索（query 向量化 → 租户隔离 ANN → PG 元数据反查）")
    public Result<List<RagSearchItem>> search(@RequestBody(required = false) RagSearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "query is required");
        }

        // Fail-closed: without a tenant the request is rejected instead of searching.
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "Tenant ID is required");
        }

        RagSearchService searchService = ragSearchServiceProvider.getIfAvailable();
        if (searchService == null) {
            return Result.error(503, "Vector search is not enabled (milvus.enabled=false)");
        }

        int topK = request.getTopK() <= 0 ? DEFAULT_TOP_K : Math.min(request.getTopK(), MAX_TOP_K);
        List<String> docTypeFilter = request.getFilters() == null ? null : request.getFilters().getDocType();

        List<KnowledgeChunk> chunks = searchService.search(request.getQuery(), tenantId, topK, docTypeFilter);
        List<RagSearchItem> items = chunks.stream().map(RagController::toItem).toList();
        return Result.success(items);
    }

    private static RagSearchItem toItem(KnowledgeChunk chunk) {
        return RagSearchItem.builder()
                .docId(parseDocId(chunk.getDocId()))
                .docName(chunk.getDocName())
                .content(chunk.getContent())
                .score(chunk.getScore())
                .metadata(chunk.getMetadata())
                .build();
    }

    private static Long parseDocId(String docId) {
        if (docId == null || docId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(docId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Data
    public static class RetrieveRequest {
        private String query;
        private int topK = 5;
    }

    @Data
    public static class RagSearchRequest {
        private String query;
        private int topK = DEFAULT_TOP_K;
        private RagSearchFilters filters;
    }

    @Data
    public static class RagSearchFilters {
        private List<String> docType;
    }
}
