package com.schemaplexai.context.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.rag.DocumentChunker;
import com.schemaplexai.context.rag.TextChunk;
import com.schemaplexai.context.service.impl.FailedStatusWriter;
import com.schemaplexai.context.service.impl.MilvusSyncServiceImpl;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilvusSyncServiceImplTest {

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

    @InjectMocks
    private MilvusSyncServiceImpl milvusSyncService;

    // ------------------------------------------------------------------
    // syncToMilvus
    // ------------------------------------------------------------------

    @Test
    void syncToMilvus_nullDocId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> milvusSyncService.syncToMilvus(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(knowledgeDocMapper, never()).selectById(any());
    }

    @Test
    void syncToMilvus_docNotFound_throwsNotFound() {
        when(knowledgeDocMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> milvusSyncService.syncToMilvus(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void syncToMilvus_success_marksAsSynced() throws Exception {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTenantId("tenant-1");
        doc.setTitle("Test Doc");
        doc.setFileName("test.txt");
        doc.setFileUrl("http://localhost:9000/documents/test.txt");
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        mockMinioObject("documents/test.txt", "real extracted text");

        when(documentChunker.chunk(any(), any())).thenReturn(List.of(
                TextChunk.builder().index(0).content("chunk1").startPosition(0).endPosition(6).build()
        ));
        when(embeddingService.embedBatch(any())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.insert(any(InsertReq.class))).thenReturn(InsertResp.builder().InsertCnt(1L).build());

        milvusSyncService.syncToMilvus(1L);

        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper).updateById(doc);
    }

    @Test
    void syncToMilvus_minioExtractionFailure_marksFailedWithoutSimulatedSync() throws Exception {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTenantId("tenant-1");
        doc.setTitle("Real Doc");
        doc.setFileName("real.pdf");
        doc.setFileUrl("http://localhost:9000/documents/real.pdf");
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("download failed"));
        ReflectionTestUtils.setField(milvusSyncService, "minioEnabled", true);
        ReflectionTestUtils.setField(milvusSyncService, "minioClient", minioClient);

        Throwable thrown = catchThrowable(() -> milvusSyncService.syncToMilvus(1L));

        assertThat(thrown).isInstanceOf(BaseException.class);
        verify(failedStatusWriter).markFailed(eq(1L), contains("download failed"));
        verify(documentChunker, never()).chunk(any(), any());
        verify(embeddingService, never()).embedBatch(any());
        verify(milvusClient, never()).insert(any(InsertReq.class));
        assertThat(doc.getStatus()).isEqualTo("PENDING");
        assertThat(doc.getSyncStatus()).isNull();
    }

    @Test
    void syncToMilvus_missingFileUrl_marksFailedWithoutSimulatedSync() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTenantId("tenant-1");
        doc.setTitle("No File Doc");
        doc.setFileName("missing.txt");
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        Throwable thrown = catchThrowable(() -> milvusSyncService.syncToMilvus(1L));

        assertThat(thrown).isInstanceOf(BaseException.class);
        verify(failedStatusWriter).markFailed(eq(1L), contains("fileUrl is required"));
        verify(documentChunker, never()).chunk(any(), any());
        verify(embeddingService, never()).embedBatch(any());
        verify(milvusClient, never()).insert(any(InsertReq.class));
        assertThat(doc.getStatus()).isEqualTo("PENDING");
        assertThat(doc.getSyncStatus()).isNull();
    }

    @Test
    void syncToMilvus_blankTenant_refusesSyncInsteadOfDefaultPartition() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Tenantless Doc");
        doc.setFileName("t.txt");
        doc.setFileUrl("http://localhost:9000/documents/t.txt");
        doc.setStatus("PENDING");
        // no tenantId set
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        Throwable thrown = catchThrowable(() -> milvusSyncService.syncToMilvus(1L));

        assertThat(thrown).isInstanceOf(BaseException.class);
        verify(failedStatusWriter).markFailed(eq(1L), contains("tenantId"));
        verify(milvusClient, never()).insert(any(InsertReq.class));
    }

    @Test
    void syncToMilvus_tenantBucketUrl_downloadsWithStrippedObjectName() throws Exception {
        // MinioFileStorageService stores URLs as {endpoint}/{sf-files-<tenant>}/{object};
        // the download must resolve the same bucket and strip it from the object name.
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTenantId("tenant-1");
        doc.setTitle("Round Trip Doc");
        doc.setFileName("file.pdf");
        doc.setFileUrl("http://localhost:9000/sf-files-tenant-1/uuid-file.pdf");
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        mockMinioObject("uuid-file.pdf", "extracted text");

        when(documentChunker.chunk(any(), any())).thenReturn(List.of(
                TextChunk.builder().index(0).content("chunk1").startPosition(0).endPosition(6).build()
        ));
        when(embeddingService.embedBatch(any())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.insert(any(InsertReq.class))).thenReturn(InsertResp.builder().InsertCnt(1L).build());

        milvusSyncService.syncToMilvus(1L);

        org.mockito.ArgumentCaptor<GetObjectArgs> captor =
                org.mockito.ArgumentCaptor.forClass(GetObjectArgs.class);
        MinioClient minio = (MinioClient) ReflectionTestUtils.getField(milvusSyncService, "minioClient");
        verify(minio).getObject(captor.capture());
        GetObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo("sf-files-tenant-1");
        assertThat(args.object()).isEqualTo("uuid-file.pdf");
        assertThat(doc.getStatus()).isEqualTo("SYNCED");
    }

    @Test
    void syncToMilvus_alreadySynced_skipsSync() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setStatus("SYNCED");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        milvusSyncService.syncToMilvus(1L);

        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper, never()).updateById(any());
    }

    // ------------------------------------------------------------------
    // deleteByDocId
    // ------------------------------------------------------------------

    @Test
    void deleteByDocId_nullDocId_throwsParamErrorWithoutMilvusCall() {
        assertThatThrownBy(() -> milvusSyncService.deleteByDocId(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(milvusProperties, never()).getCollectionName();
        verify(milvusClient, never()).delete(any(DeleteReq.class));
    }

    @Test
    void deleteByDocId_success_callsMilvusDelete() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.delete(any(DeleteReq.class))).thenReturn(DeleteResp.builder().deleteCnt(3L).build());

        milvusSyncService.deleteByDocId(1L);

        verify(milvusClient).delete(argThat(req ->
            req.getCollectionName().equals("test_collection") &&
            req.getFilter().contains("doc_id == '1'")
        ));
    }

    // ------------------------------------------------------------------
    // reSyncDoc
    // ------------------------------------------------------------------

    @Test
    void reSyncDoc_nullDocId_throwsParamErrorWithoutLookupOrMilvusCall() {
        assertThatThrownBy(() -> milvusSyncService.reSyncDoc(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(knowledgeDocMapper, never()).selectById(any());
        verify(milvusClient, never()).delete(any(DeleteReq.class));
        verify(milvusClient, never()).insert(any(InsertReq.class));
    }

    @Test
    void reSyncDoc_success_deletesThenReInserts() throws Exception {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTenantId("tenant-1");
        doc.setTitle("Test Doc");
        doc.setFileName("test.txt");
        doc.setFileUrl("http://localhost:9000/documents/test.txt");
        doc.setStatus("SYNCED");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.delete(any(DeleteReq.class))).thenReturn(DeleteResp.builder().deleteCnt(2L).build());
        mockMinioObject("documents/test.txt", "real extracted text");
        when(documentChunker.chunk(any(), any())).thenReturn(List.of(
                TextChunk.builder().index(0).content("chunk1").startPosition(0).endPosition(6).build()
        ));
        when(embeddingService.embedBatch(any())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(milvusClient.insert(any(InsertReq.class))).thenReturn(InsertResp.builder().InsertCnt(1L).build());

        milvusSyncService.reSyncDoc(1L);

        verify(milvusClient).delete(any(DeleteReq.class));
        verify(milvusClient).insert(any(InsertReq.class));
        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        assertThat(doc.getSyncStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper).updateById(doc);
    }

    @Test
    void reSyncDoc_docNotFound_throwsNotFound() {
        when(knowledgeDocMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> milvusSyncService.reSyncDoc(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    private void mockMinioObject(String objectName, String content) throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(
                        Headers.of(),
                        "sf-files-tenant-1",
                        null,
                        objectName,
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
        ReflectionTestUtils.setField(milvusSyncService, "minioEnabled", true);
        ReflectionTestUtils.setField(milvusSyncService, "minioClient", minioClient);
    }
}
