package com.schemaplexai.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the context service's internal Milvus-sync endpoints. The task module
 * consumes {@code sf.milvus.sync.queue} but the seven-step sync pipeline (PG → MinIO →
 * Tika → chunking → embedding → Milvus → PG status) lives in {@code schemaplexai-context},
 * so the consumer delegates across the service boundary here.
 */
@Slf4j
public class ContextServiceClient {

    static final String SYNC_PATH = "/context/internal/milvus-sync/{docId}";
    static final String VECTOR_COUNT_PATH = "/context/internal/milvus-sync/docs/{docId}/vector-count";

    private final RestClient restClient;

    public ContextServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Triggers the idempotent seven-step sync for a document in the context service.
     *
     * @throws BaseException when the call fails or the context service reports an error
     */
    public void syncDocument(Long docId, String tenantId) {
        JsonNode body = restClient.post()
                .uri(SYNC_PATH, docId)
                .header(CommonConstants.HEADER_TENANT_ID, tenantId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new BaseException(ResultCode.INTERNAL_ERROR,
                            "context service returned HTTP " + response.getStatusCode().value()
                                    + " while syncing doc " + docId);
                })
                .body(JsonNode.class);
        requireSuccess(body, "sync doc " + docId);
        log.info("[ContextServiceClient] context service completed milvus sync for docId={}", docId);
    }

    /**
     * Returns the number of vectors the context service's Milvus currently stores for the
     * document (tenant-scoped). Used by the daily reconciliation to detect drifted docs.
     *
     * @throws BaseException when the call fails, including when Milvus is disabled in the
     *                       context service (HTTP 200 with code 503 in the Result envelope)
     */
    public long countVectors(Long docId, String tenantId) {
        JsonNode body = restClient.get()
                .uri(VECTOR_COUNT_PATH, docId)
                .header(CommonConstants.HEADER_TENANT_ID, tenantId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new BaseException(ResultCode.INTERNAL_ERROR,
                            "context service returned HTTP " + response.getStatusCode().value()
                                    + " while counting vectors for doc " + docId);
                })
                .body(JsonNode.class);
        requireSuccess(body, "count vectors for doc " + docId);
        return body.path("data").asLong(0L);
    }

    private void requireSuccess(JsonNode body, String operation) {
        if (body == null) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    operation + ": empty response from context service");
        }
        int code = body.path("code").asInt(-1);
        if (code != ResultCode.SUCCESS.getCode()) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    operation + ": context service returned code=" + code
                            + ", message=" + body.path("message").asText());
        }
    }
}
