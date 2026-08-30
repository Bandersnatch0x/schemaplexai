package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.milvus.MilvusVectorCounter;
import com.schemaplexai.context.service.MilvusSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints consumed by the {@code schemaplexai-task} module:
 * <ul>
 *   <li>{@code POST /context/internal/milvus-sync/{docId}} — the MQ consumer
 *       ({@code sf.milvus.sync.queue}) delegates the spec §3.4 seven-step sync here.</li>
 *   <li>{@code GET /context/internal/milvus-sync/docs/{docId}/vector-count} — the daily
 *       reconciliation compares per-document Milvus vector counts against PG state.</li>
 * </ul>
 * Both endpoints are fail-closed on a missing tenant. Because PG lookups inside
 * {@link MilvusSyncService} are tenant-scoped, a caller can only operate on documents
 * belonging to the tenant it presents.
 */
@RestController
@RequestMapping("/context/internal/milvus-sync")
@RequiredArgsConstructor
@Tag(name = "内部接口-Milvus同步", description = "供任务调度服务调用的知识文档同步与对账接口")
public class InternalMilvusSyncController {

    private final MilvusSyncService milvusSyncService;
    /** Present only when milvus.enabled=true. */
    private final ObjectProvider<MilvusVectorCounter> vectorCounterProvider;

    /**
     * Ensures the given document's vectors in Milvus match PG: existing vectors for the
     * document are deleted first, then the full pipeline re-runs (PG lookup → MinIO
     * download → Tika extraction → chunking → embedding → Milvus insert → PG status).
     * Delete-first makes repeated MQ deliveries/reconciliation retries idempotent.
     */
    @PostMapping("/{docId}")
    @Operation(summary = "触发文档的 Milvus 同步（幂等：先删后写）")
    public Result<Boolean> ensureSynced(@PathVariable Long docId) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "Tenant ID is required");
        }
        milvusSyncService.reSyncDoc(docId);
        return Result.success(true);
    }

    /**
     * Returns how many vectors Milvus currently stores for the document (tenant-scoped).
     * Returns 503 when Milvus is disabled so reconciliation can skip the comparison.
     */
    @GetMapping("/docs/{docId}/vector-count")
    @Operation(summary = "查询文档在 Milvus 中的向量数量（对账用）")
    public Result<Long> vectorCount(@PathVariable Long docId) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "Tenant ID is required");
        }
        MilvusVectorCounter counter = vectorCounterProvider.getIfAvailable();
        if (counter == null) {
            return Result.error(503, "Milvus is disabled; vector count unavailable");
        }
        return Result.success(counter.countByDocId(docId, tenantId));
    }
}
