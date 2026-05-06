package com.schemaplexai.context.milvus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.context.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusCollectionInitializer {

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        String collectionName = milvusProperties.getCollectionName();
        try {
            boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());

            if (exists) {
                log.info("Milvus collection '{}' already exists, skipping creation", collectionName);
                return;
            }

            log.info("Milvus collection '{}' does not exist, creating from schema...", collectionName);
            createCollectionFromSchema(collectionName);
            log.info("Milvus collection '{}' created successfully", collectionName);
        } catch (Exception e) {
            log.error("Failed to initialize Milvus collection '{}': {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Milvus collection initialization failed", e);
        }
    }

    private void createCollectionFromSchema(String collectionName) throws Exception {
        ClassPathResource resource = new ClassPathResource("milvus/knowledge_doc_collection.json");
        try (InputStream is = resource.getInputStream()) {
            JsonNode schema = objectMapper.readTree(is);

            // Build fields
            List<CreateCollectionReq.FieldSchema> fieldSchemas = new ArrayList<>();
            JsonNode fieldsNode = schema.get("fields");
            for (JsonNode fieldNode : fieldsNode) {
                CreateCollectionReq.FieldSchema fieldSchema = parseFieldSchema(fieldNode);
                fieldSchemas.add(fieldSchema);
            }

            // Build collection schema
            CreateCollectionReq.CollectionSchema collectionSchema = CreateCollectionReq.CollectionSchema.builder()
                    .fieldSchemaList(fieldSchemas)
                    .build();

            // Create collection with partition key
            CreateCollectionReq.CreateCollectionReqBuilder createBuilder = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(collectionSchema);

            // Check if any field is a partition key
            for (JsonNode fieldNode : fieldsNode) {
                if (fieldNode.has("isPartitionKey") && fieldNode.get("isPartitionKey").asBoolean()) {
                    createBuilder.numPartitions(64);
                    break;
                }
            }

            milvusClient.createCollection(createBuilder.build());

            // Create indexes
            JsonNode indexesNode = schema.get("indexes");
            if (indexesNode != null) {
                for (JsonNode indexNode : indexesNode) {
                    createIndex(collectionName, indexNode);
                }
            }
        }
    }

    private CreateCollectionReq.FieldSchema parseFieldSchema(JsonNode fieldNode) {
        String name = fieldNode.get("name").asText();
        String dataType = fieldNode.get("dataType").asText();

        CreateCollectionReq.FieldSchema.FieldSchemaBuilder builder = CreateCollectionReq.FieldSchema.builder()
                .name(name)
                .dataType(parseDataType(dataType));

        if (fieldNode.has("isPrimary") && fieldNode.get("isPrimary").asBoolean()) {
            builder.isPrimaryKey(true);
            builder.autoID(false);
        }

        if (fieldNode.has("maxLength")) {
            builder.maxLength(fieldNode.get("maxLength").asInt());
        }

        if (fieldNode.has("dimension")) {
            builder.dimension(fieldNode.get("dimension").asInt());
        }

        if (fieldNode.has("isPartitionKey") && fieldNode.get("isPartitionKey").asBoolean()) {
            builder.isPartitionKey(true);
        }

        return builder.build();
    }

    private io.milvus.v2.common.DataType parseDataType(String dataType) {
        return io.milvus.v2.common.DataType.valueOf(dataType);
    }

    private void createIndex(String collectionName, JsonNode indexNode) {
        String fieldName = indexNode.get("fieldName").asText();
        String indexType = indexNode.get("indexType").asText();
        String metricType = indexNode.get("metricType").asText();

        IndexParam.IndexParamBuilder<?, ?> indexParamBuilder = IndexParam.builder()
                .fieldName(fieldName)
                .indexType(IndexParam.IndexType.valueOf(indexType))
                .metricType(IndexParam.MetricType.valueOf(metricType));

        if (indexNode.has("params")) {
            JsonNode paramsNode = indexNode.get("params");
            Map<String, Object> extras = new HashMap<>();
            Iterator<String> fieldNames = paramsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                extras.put(key, paramsNode.get(key).asText());
            }
            indexParamBuilder.extraParams(extras);
        }

        milvusClient.createIndex(CreateIndexReq.builder()
                .collectionName(collectionName)
                .indexParams(List.of(indexParamBuilder.build()))
                .build());

        log.info("Created index on field '{}' for collection '{}'", fieldName, collectionName);
    }
}
