package com.schemaplexai.context.milvus;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Counts stored vectors for a knowledge document by its PG primary key, scoped by tenant.
 * Used by the daily Milvus↔PG reconciliation (spec §3.5) to detect documents that PG
 * considers indexed but whose vectors are missing from Milvus.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusVectorCounter {

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+$");
    private static final String COUNT_FIELD = "count(*)";

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;

    /**
     * @return number of vectors stored for {@code docId} under {@code tenantId}
     */
    public long countByDocId(Long docId, String tenantId) {
        if (docId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        if (tenantId == null || tenantId.isBlank() || !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "valid tenantId is required for vector count");
        }

        QueryReq request = QueryReq.builder()
                .collectionName(milvusProperties.getCollectionName())
                .filter("doc_id == \"" + docId + "\" and tenant_id == \"" + tenantId + "\"")
                .outputFields(List.of(COUNT_FIELD))
                .consistencyLevel(milvusProperties.getConsistencyLevel())
                .build();

        try {
            QueryResp response = milvusClient.query(request);
            return parseCount(response);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Milvus vector count failed for docId={}, tenantId={}: {}", docId, tenantId, e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Milvus vector count failed for doc " + docId + ": " + e.getMessage(), e);
        }
    }

    private long parseCount(QueryResp response) {
        if (response == null || response.getQueryResults() == null || response.getQueryResults().isEmpty()) {
            return 0L;
        }
        for (QueryResp.QueryResult result : response.getQueryResults()) {
            Map<String, Object> entity = result.getEntity();
            if (entity == null) {
                continue;
            }
            Object count = entity.get(COUNT_FIELD);
            if (count == null) {
                // Defensive: fall back to any numeric value in the row.
                count = entity.values().stream().filter(v -> v instanceof Number).findFirst().orElse(null);
            }
            if (count instanceof Number number) {
                return number.longValue();
            }
            if (count != null) {
                try {
                    return Long.parseLong(count.toString());
                } catch (NumberFormatException ignored) {
                    // fall through to next row
                }
            }
        }
        return 0L;
    }
}
