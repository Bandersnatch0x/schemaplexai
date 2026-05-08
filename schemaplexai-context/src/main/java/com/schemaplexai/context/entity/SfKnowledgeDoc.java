package com.schemaplexai.context.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_knowledge_doc")
public class SfKnowledgeDoc extends BaseEntity {

    private String title;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String status;
    private String syncStatus;
    private String docType;
}
