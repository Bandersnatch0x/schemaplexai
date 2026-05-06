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
        save(doc);
        log.info("Knowledge doc uploaded: {}, triggering vectorization", doc.getId());
        milvusSyncService.syncToMilvus(doc.getId());
    }
}
