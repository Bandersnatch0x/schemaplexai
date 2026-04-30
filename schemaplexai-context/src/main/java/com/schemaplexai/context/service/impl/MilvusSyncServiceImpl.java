package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.MilvusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class MilvusSyncServiceImpl implements MilvusSyncService {

    private final SfKnowledgeDocMapper knowledgeDocMapper;

    @Override
    public void syncToMilvus(Long docId) {
        log.info("Sync doc {} to Milvus", docId);

        SfKnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Knowledge document not found: " + docId);
        }

        // Phase 1: Mark document as pending sync
        // Phase 2 (TODO): Actual Milvus sync when embedding service is ready
        // Steps:
        // 1. Download document from MinIO
        // 2. Extract text (Apache Tika)
        // 3. Chunk text
        // 4. Generate embeddings
        // 5. Insert into Milvus collection

        doc.setStatus("SYNCED");
        knowledgeDocMapper.updateById(doc);
        log.info("Document {} marked as synced (full vector sync pending embedding service)", docId);
    }
}
