package com.schemaplexai.context.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {

    private String docId;
    private Integer chunkIndex;
    private String content;
    private Float score;
    private String tenantId;
}
