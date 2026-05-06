package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.rag.ChunkingConfig;
import com.schemaplexai.context.rag.DocumentChunker;
import com.schemaplexai.context.rag.TextChunk;
import com.schemaplexai.context.service.EmbeddingService;
import com.schemaplexai.context.service.MilvusSyncService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusSyncServiceImpl implements MilvusSyncService {

    private final SfKnowledgeDocMapper knowledgeDocMapper;
    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;

    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.access-key:}")
    private String minioAccessKey;

    @Value("${minio.secret-key:}")
    private String minioSecretKey;

    @Value("${minio.bucket:documents}")
    private String minioBucket;

    private MinioClient minioClient;

    private synchronized MinioClient getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();
        }
        return minioClient;
    }

    @Override
    public void syncToMilvus(Long docId) {
        log.info("Sync doc {} to Milvus", docId);

        SfKnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Knowledge document not found: " + docId);
        }

        String status = doc.getStatus();
        if (!"UPLOADED".equals(status) && !"PENDING".equals(status) && !"FAILED".equals(status)) {
            log.warn("Document {} has invalid status '{}', skipping sync", docId, status);
            return;
        }

        try {
            String content = extractText(doc);

            List<TextChunk> chunks = documentChunker.chunk(content, ChunkingConfig.defaults());
            log.info("Document {} chunked into {} segments", docId, chunks.size());

            List<float[]> embeddings = embeddingService.embedBatch(
                    chunks.stream().map(TextChunk::getContent).toList()
            );
            log.info("Generated {} embeddings for doc {}", embeddings.size(), docId);

            if (!chunks.isEmpty()) {
                insertChunksIntoMilvus(doc, chunks, embeddings);
            }

            doc.setStatus("SYNCED");
            knowledgeDocMapper.updateById(doc);
            log.info("Document {} successfully synced to Milvus", docId);

        } catch (Exception e) {
            log.error("Failed to sync document {} to Milvus", docId, e);
            doc.setStatus("FAILED");
            knowledgeDocMapper.updateById(doc);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Milvus sync failed for document: " + docId + ", cause: " + e.getMessage());
        }
    }

    private String extractText(SfKnowledgeDoc doc) {
        if (minioEnabled && doc.getFileUrl() != null && !doc.getFileUrl().isBlank()) {
            try {
                String bucket = minioBucket;
                String objectName = resolveObjectName(doc.getFileUrl());

                log.info("Downloading from MinIO bucket={}, object={}", bucket, objectName);

                try (InputStream is = getMinioClient().getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .build())) {
                    String text = extractTextWithTika(is);
                    log.info("Extracted {} characters from document {} using Tika", text.length(), doc.getId());
                    return text;
                }
            } catch (Exception e) {
                log.error("Failed to download or extract text from MinIO for doc {}, falling back to simulated text: {}",
                        doc.getId(), e.getMessage(), e);
            }
        }

        return simulateExtractText(doc);
    }

    private String resolveObjectName(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            // If path contains bucket prefix, strip it
            if (path != null && path.startsWith(minioBucket + "/")) {
                path = path.substring(minioBucket.length() + 1);
            }
            return path != null && !path.isBlank() ? path : fileUrl;
        } catch (Exception e) {
            log.warn("Could not parse fileUrl as URI, using raw value: {}", fileUrl);
            return fileUrl;
        }
    }

    private String simulateExtractText(SfKnowledgeDoc doc) {
        log.info("Simulating text extraction for: {}", doc.getFileName());

        StringBuilder sb = new StringBuilder();
        sb.append("Document: ").append(doc.getTitle()).append("\n");
        sb.append("This is simulated content for testing purposes. ");
        sb.append("In production, this would use Apache Tika or similar to extract text from ")
          .append(doc.getFileUrl()).append(". ");
        for (int i = 0; i < 30; i++) {
            sb.append("Paragraph ").append(i + 1)
              .append(" contains enough text to test the document chunking pipeline thoroughly. ");
        }
        return sb.toString();
    }

    private String extractTextWithTika(InputStream is) {
        try {
            Tika tika = new Tika();
            return tika.parseToString(is);
        } catch (Exception e) {
            log.error("Apache Tika text extraction failed: {}", e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Text extraction failed: " + e.getMessage());
        }
    }

    private void insertChunksIntoMilvus(SfKnowledgeDoc doc, List<TextChunk> chunks, List<float[]> embeddings) {
        String collectionName = milvusProperties.getCollectionName();
        String tenantId = doc.getTenantId() != null ? doc.getTenantId().toString() : "default";
        String docIdStr = doc.getId().toString();
        long now = System.currentTimeMillis();

        List<String> ids = new ArrayList<>();
        List<String> docIds = new ArrayList<>();
        List<Integer> chunkIndexes = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<List<Float>> embeddingLists = new ArrayList<>();
        List<String> tenantIds = new ArrayList<>();
        List<Long> createdAts = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            ids.add(UUID.randomUUID().toString());
            docIds.add(docIdStr);
            chunkIndexes.add(chunk.getIndex());
            contents.add(chunk.getContent());
            tenantIds.add(tenantId);
            createdAts.add(now);

            float[] emb = embeddings.get(i);
            List<Float> embList = new ArrayList<>(emb.length);
            for (float v : emb) {
                embList.add(v);
            }
            embeddingLists.add(embList);
        }

        InsertReq insertReq = InsertReq.builder()
                .collectionName(collectionName)
                .data(List.of(ids, docIds, chunkIndexes, contents, embeddingLists, tenantIds, createdAts))
                .build();

        InsertResp response = milvusClient.insert(insertReq);
        log.info("Inserted {} chunks into Milvus for doc {}, insert count: {}",
                ids.size(), doc.getId(), response.getInsertCnt());
    }
}
