package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.KnowledgeDocService;
import com.schemaplexai.context.service.MilvusSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Service
public class KnowledgeDocServiceImpl extends ServiceImpl<SfKnowledgeDocMapper, SfKnowledgeDoc> implements KnowledgeDocService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocServiceImpl.class);

    private final MilvusSyncService milvusSyncService;

    public KnowledgeDocServiceImpl(MilvusSyncService milvusSyncService) {
        this.milvusSyncService = milvusSyncService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadAndVectorize(SfKnowledgeDoc doc) {
        doc.setStatus("UPLOADED");
        doc.setSyncStatus("PENDING");
        save(doc);
        log.info("Knowledge doc uploaded: {}, triggering vectorization", doc.getId());
        milvusSyncService.syncToMilvus(doc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        Long docId = (Long) id;
        log.info("Removing knowledge doc {} and cleaning Milvus vectors", docId);
        try {
            milvusSyncService.deleteByDocId(docId);
        } catch (Exception e) {
            log.warn("Failed to delete Milvus vectors for doc {}, proceeding with DB removal: {}", docId, e.getMessage());
        }
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SfKnowledgeDoc entity) {
        boolean updated = super.updateById(entity);
        if (updated && entity.getId() != null) {
            log.info("Knowledge doc {} updated, triggering re-vectorization", entity.getId());
            try {
                milvusSyncService.reSyncDoc(entity.getId());
            } catch (Exception e) {
                log.error("Failed to re-sync doc {} to Milvus after update: {}", entity.getId(), e.getMessage());
            }
        }
        return updated;
    }
}
