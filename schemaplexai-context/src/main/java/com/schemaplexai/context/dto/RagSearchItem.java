package com.schemaplexai.context.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response item of {@code POST /context/rag/search} (spec §4.1):
 * docId / docName / content / score / metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "向量检索结果条目")
public class RagSearchItem {

    @Schema(description = "知识文档 ID（PG 主键）")
    private Long docId;

    @Schema(description = "文档名称")
    private String docName;

    @Schema(description = "命中的分块内容")
    private String content;

    @Schema(description = "相似度得分（Milvus 距离）")
    private Float score;

    @Schema(description = "文档元数据（如 docType）")
    private Map<String, Object> metadata;
}
