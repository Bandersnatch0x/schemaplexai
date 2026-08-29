package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.MilvusSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Real {@code sf.milvus.sync} handler: delegates the spec §3.4 seven-step sync
 * (PG → MinIO → Tika → chunking → embedding → Milvus → PG status update) to the
 * context service, where the pipeline and its data sources live. The task module
 * contributes the durable MQ plane (manual ACK, DLQ, fail log, idempotency).
 * <p>
 * Failures propagate so the consumer nacks the message into the dead-letter queue
 * and records it in {@code sf_message_fail_log} for manual retry; the daily
 * reconciliation additionally re-dispatches docs stuck in PENDING/FAILED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpMilvusSyncRequestHandler implements MilvusSyncRequestHandler {

    private final ContextServiceClient contextServiceClient;

    @Override
    public void handle(MilvusSyncMessage message) {
        if (message == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "milvus sync message is required");
        }
        if (message.getDocId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required for milvus sync");
        }
        // Fail-closed tenant boundary: never sync without an explicit tenant scope.
        if (message.getTenantId() == null || message.getTenantId().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "tenantId is required for milvus sync of doc " + message.getDocId());
        }

        log.info("[MilvusSyncRequestHandler] Delegating milvus sync to context service: docId={}, tenantId={}",
                message.getDocId(), message.getTenantId());
        contextServiceClient.syncDocument(message.getDocId(), message.getTenantId());
        log.info("[MilvusSyncRequestHandler] Milvus sync completed for docId={}", message.getDocId());
    }
}
