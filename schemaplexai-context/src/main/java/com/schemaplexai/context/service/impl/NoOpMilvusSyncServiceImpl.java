package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.MilvusSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link MilvusSyncService} used when Milvus is disabled.
 * Allows {@link KnowledgeDocServiceImpl} to function without a Milvus connection.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMilvusSyncServiceImpl implements MilvusSyncService {

    @Override
    public void syncToMilvus(Long docId) {
        if (docId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        log.warn("Milvus is disabled. Document {} will not be vectorized.", docId);
    }

    @Override
    public void deleteByDocId(Long docId) {
        if (docId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        log.warn("Milvus is disabled. Document {} vectors will not be deleted.", docId);
    }

    @Override
    public void reSyncDoc(Long docId) {
        if (docId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        log.warn("Milvus is disabled. Document {} will not be re-vectorized.", docId);
    }
}
