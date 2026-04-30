package com.schemaplexai.context.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_knowledge_doc_version")
public class SfKnowledgeDocVersion extends BaseEntity {

    private Long docId;
    private String version;
    private String fileUrl;
    private String changeLog;
}
