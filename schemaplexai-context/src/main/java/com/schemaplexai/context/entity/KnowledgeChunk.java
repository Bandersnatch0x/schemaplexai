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

    /** Document name resolved by the PG back-query (spec §4.2 step 3). */
    private String docName;

    /** Document metadata resolved by the PG back-query (e.g. docType). */
    private java.util.Map<String, Object> metadata;
}
