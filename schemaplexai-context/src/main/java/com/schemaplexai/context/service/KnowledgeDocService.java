package com.schemaplexai.context.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.context.entity.SfKnowledgeDoc;

public interface KnowledgeDocService extends IService<SfKnowledgeDoc> {

    void uploadAndVectorize(SfKnowledgeDoc doc);
}
