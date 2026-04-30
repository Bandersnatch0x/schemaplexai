package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.KnowledgeDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class KnowledgeDocServiceImpl extends ServiceImpl<SfKnowledgeDocMapper, SfKnowledgeDoc> implements KnowledgeDocService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadAndVectorize(SfKnowledgeDoc doc) {
        save(doc);
        log.info("Knowledge doc uploaded: {}, pending vectorization", doc.getId());
    }
}
