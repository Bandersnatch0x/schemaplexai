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
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    void syncToMilvus_docNotFound_throwsNotFound() {
        when(knowledgeDocMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> milvusSyncService.syncToMilvus(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void syncToMilvus_success_marksAsSynced() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Test Doc");
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);
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
    void syncToMilvus_alreadySynced_skipsSync() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setStatus("SYNCED");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        milvusSyncService.syncToMilvus(1L);

        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper, never()).updateById(any());
    }
}
