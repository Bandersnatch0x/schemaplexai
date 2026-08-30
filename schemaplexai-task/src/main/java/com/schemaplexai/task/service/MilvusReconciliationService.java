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

/**
 * Daily Milvus↔PG reconciliation (spec §3.5):
 * <ol>
 *   <li>Re-dispatch docs still PENDING/FAILED so the (now real) consumer syncs them.</li>
 *   <li>Compare per-document: query Milvus by the PG primary key and re-dispatch any doc
 *       that PG considers SYNCED but whose vectors are missing from Milvus.</li>
 * </ol>
 * Repair messages go through the normal {@code sf.milvus.sync} route, i.e. the fixed
 * consumer, and the context-service endpoint deletes existing vectors first, so repeated
 * repair runs are idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusReconciliationService {

    private final MilvusReconciliationMapper reconciliationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ContextServiceClient contextServiceClient;

    @Value("${task.milvus-reconciliation.collection-name:knowledge_doc_embedding}")
    private String collectionName = "knowledge_doc_embedding";

    /**
     * @return total number of sync requests dispatched (pending re-dispatches + drift repairs)
     */
    public int reconcilePendingDocuments(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("milvus reconciliation batchSize must be positive");
        }

        int dispatched = redispatchPendingDocuments(batchSize);
        dispatched += verifySyncedDocumentsAgainstMilvus(batchSize);
        return dispatched;
    }

    private int redispatchPendingDocuments(int batchSize) {
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
        log.info("[MilvusReconciliationService] Dispatched {} pending-document sync requests", dispatched);
        return dispatched;
    }

    /**
     * Real PG↔Milvus comparison: for each doc marked SYNCED, query Milvus by the document
     * primary key (tenant-scoped). Zero stored vectors means drift — re-dispatch a repair.
     * If the count endpoint is unavailable (context service or Milvus down/disabled) the
     * comparison is skipped for this run instead of failing the job.
     */
    private int verifySyncedDocumentsAgainstMilvus(int batchSize) {
        List<PendingMilvusDocument> synced = reconciliationMapper.findSyncedDocuments(batchSize);
        if (synced == null || synced.isEmpty()) {
            return 0;
        }

        int repaired = 0;
        for (PendingMilvusDocument document : synced) {
            long vectorCount;
            try {
                vectorCount = contextServiceClient.countVectors(document.getDocId(), document.getTenantId());
            } catch (Exception e) {
                log.warn("[MilvusReconciliationService] Vector-count check unavailable "
                        + "(context service/Milvus unreachable or disabled); skipping remaining comparisons: {}",
                        e.getMessage());
                break;
            }
            if (vectorCount <= 0) {
                log.warn("[MilvusReconciliationService] Doc {} is SYNCED in PG but has 0 vectors in Milvus;"
                        + " re-dispatching repair", document.getDocId());
                publishSyncRequest(document);
                repaired++;
            }
        }
        if (repaired > 0) {
            log.info("[MilvusReconciliationService] Repaired {} drifted documents", repaired);
        }
        return repaired;
    }

    private void publishSyncRequest(PendingMilvusDocument document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("collectionName", collectionName);
        payload.put("operation", "SYNC_DOC");
        payload.put("docId", document.getDocId());
        payload.put("tenantId", document.getTenantId());
        // Unique per dispatch: the MQ idempotency layer deduplicates on the message body,
        // so a fixed key would silently swallow every later retry of the same document.
        payload.put("idempotencyKey",
                "milvus-sync-doc-" + document.getDocId() + "-" + System.currentTimeMillis());

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
