package com.schemaplexai.task.mapper;

import com.schemaplexai.task.service.PendingMilvusDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
