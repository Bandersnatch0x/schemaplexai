package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
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
        if (doc == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "knowledge doc is required");
        }
        if (doc.getTitle() == null || doc.getTitle().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "title is required");
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tenant ID is required");
        }
        doc.setTenantId(tenantId);
        doc.setStatus("UPLOADED");
        doc.setSyncStatus("PENDING");
        save(doc);
        log.info("Knowledge doc uploaded: {}, triggering vectorization", doc.getId());
        milvusSyncService.syncToMilvus(doc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        Long docId = (Long) id;
        return removeKnowledgeDocById(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(SfKnowledgeDoc entity) {
        if (entity == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "knowledge doc is required");
        }
        if (entity.getId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        return removeKnowledgeDocById(entity.getId());
    }

    private boolean removeKnowledgeDocById(Long docId) {
        log.info("Removing knowledge doc {} and cleaning Milvus vectors", docId);
        try {
            milvusSyncService.deleteByDocId(docId);
        } catch (Exception e) {
            log.warn("Failed to delete Milvus vectors for doc {}, proceeding with DB removal: {}", docId, e.getMessage());
        }
        return super.removeById((Serializable) docId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SfKnowledgeDoc entity) {
        if (entity == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "knowledge doc is required");
        }
        if (entity.getId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
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
