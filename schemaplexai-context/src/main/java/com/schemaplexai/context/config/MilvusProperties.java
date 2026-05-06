package com.schemaplexai.context.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    private String host = "localhost";
    private Integer port = 19530;
    private String token;
    private String databaseName = "default";
    private String collectionName = "knowledge_doc_embedding";
}
