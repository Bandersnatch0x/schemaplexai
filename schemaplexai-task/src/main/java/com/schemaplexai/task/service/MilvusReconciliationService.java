package com.schemaplexai.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mapper.MilvusReconciliationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusReconciliationService {

    private final MilvusReconciliationMapper reconciliationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${task.milvus-reconciliation.collection-name:knowledge_doc_embedding}")
    private String collectionName = "knowledge_doc_embedding";

    public int reconcilePendingDocuments(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("milvus reconciliation batchSize must be positive");
        }

        List<PendingMilvusDocument> documents = reconciliationMapper.findPendingDocuments(batchSize);
        if (documents == null || documents.isEmpty()) {
            log.info("[MilvusReconciliationService] No pending Milvus documents to reconcile");
            return 0;
        }

        int dispatched = 0;
        for (PendingMilvusDocument document : documents) {
            publishSyncRequest(document);
            dispatched++;
        }
        log.info("[MilvusReconciliationService] Dispatched {} Milvus reconciliation requests", dispatched);
        return dispatched;
    }

    private void publishSyncRequest(PendingMilvusDocument document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("collectionName", collectionName);
        payload.put("operation", "SYNC_DOC");
        payload.put("docId", document.getDocId());
        payload.put("tenantId", document.getTenantId());
        payload.put("idempotencyKey", "milvus-sync-doc-" + document.getDocId());

        try {
            String body = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(
                    CommonConstants.EXCHANGE_SCHEMAPLEXAI,
                    CommonConstants.RK_MILVUS_SYNC,
                    body
            );
        } catch (JsonProcessingException e) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "failed to serialize Milvus reconciliation request for docId=" + document.getDocId(), e);
        }
    }
}
