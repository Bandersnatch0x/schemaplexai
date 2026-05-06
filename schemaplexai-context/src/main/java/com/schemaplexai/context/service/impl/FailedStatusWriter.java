package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes terminal FAILED status in an isolated REQUIRES_NEW sub-transaction so the
 * status update commits even when the outer Milvus-sync transaction rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FailedStatusWriter {

    private final SfKnowledgeDocMapper knowledgeDocMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long docId, String reason) {
        SfKnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            log.warn("Cannot mark doc {} as FAILED: document not found", docId);
            return;
        }
        doc.setStatus("FAILED");
        knowledgeDocMapper.updateById(doc);
        log.info("Marked doc {} as FAILED in independent transaction. Reason: {}", docId, reason);
    }
}
