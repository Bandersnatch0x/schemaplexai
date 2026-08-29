package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.rag.DocumentChunker;
import com.schemaplexai.context.service.EmbeddingService;
import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the MinIO object-name resolution fix: URLs written by
 * {@link MinioFileStorageService} ({@code {endpoint}/{bucket}/{object}}) must round-trip
 * back to the bare object name, including tenant-scoped buckets.
 */
@ExtendWith(MockitoExtension.class)
class MilvusSyncObjectNameResolutionTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;
    @Mock
    private DocumentChunker documentChunker;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private MilvusClientV2 milvusClient;
    @Mock
    private MilvusProperties milvusProperties;
    @Mock
    private FailedStatusWriter failedStatusWriter;

    private MilvusSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MilvusSyncServiceImpl(knowledgeDocMapper, documentChunker, embeddingService,
                milvusClient, milvusProperties, failedStatusWriter);
        ReflectionTestUtils.setField(service, "minioDefaultBucket", "documents");
    }

    @Test
    void tenantBucketUrl_stripsTenantBucketSegment() {
        String resolved = service.resolveObjectName(
                "http://localhost:9000/sf-files-tenant-1/uuid-file.pdf", "sf-files-tenant-1");

        assertThat(resolved).isEqualTo("uuid-file.pdf");
    }

    @Test
    void defaultBucketUrl_stripsDefaultBucketSegment() {
        String resolved = service.resolveObjectName(
                "http://localhost:9000/documents/report.docx", "documents");

        assertThat(resolved).isEqualTo("report.docx");
    }

    @Test
    void unknownBucketPath_keepsLegacyDefaultStrip() {
        // bucket argument does not match the URL path -> fall back to default-bucket strip
        String resolved = service.resolveObjectName(
                "http://localhost:9000/documents/report.docx", "sf-files-tenant-9");

        assertThat(resolved).isEqualTo("report.docx");
    }

    @Test
    void plainObjectNameWithoutBucket_isReturnedAsIs() {
        String resolved = service.resolveObjectName("just/an/object.txt", "sf-files-tenant-1");

        assertThat(resolved).isEqualTo("just/an/object.txt");
    }

    @Test
    void unparseableUri_returnsRawValue() {
        String raw = "://not a uri";

        assertThat(service.resolveObjectName(raw, "sf-files-tenant-1")).isEqualTo(raw);
    }

    @Test
    void resolveTenantBucket_withTenant_prefixesSfFiles() {
        assertThat(service.resolveTenantBucket("tenant-1")).isEqualTo("sf-files-tenant-1");
        assertThat(service.resolveTenantBucket(null)).isEqualTo("documents");
        assertThat(service.resolveTenantBucket("  ")).isEqualTo("documents");
    }
}
