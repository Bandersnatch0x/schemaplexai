package com.schemaplexai.task.mapper;

import com.schemaplexai.task.service.PendingMilvusDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Cross-tenant reconciliation queries. The task module deliberately registers no
 * MyBatis-Plus tenant interceptor: the daily job repairs documents across all tenants,
 * and every dispatched repair message carries its own tenantId which the consumer side
 * enforces (TenantMqFilter + tenant-scoped context-service calls).
 */
@Mapper
public interface MilvusReconciliationMapper {

    @Select("""
            SELECT id AS doc_id, tenant_id AS tenant_id
            FROM sf_knowledge_doc
            WHERE deleted = 0
              AND COALESCE(sync_status, 'PENDING') IN ('PENDING', 'FAILED')
            ORDER BY updated_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<PendingMilvusDocument> findPendingDocuments(@Param("limit") int limit);

    /**
     * Documents PG considers already synced — candidates for the Milvus vector-count
     * comparison that detects "PG says indexed but Milvus lost the vectors" drift.
     */
    @Select("""
            SELECT id AS doc_id, tenant_id AS tenant_id
            FROM sf_knowledge_doc
            WHERE deleted = 0
              AND sync_status = 'SYNCED'
            ORDER BY updated_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<PendingMilvusDocument> findSyncedDocuments(@Param("limit") int limit);
}
