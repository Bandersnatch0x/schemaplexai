package com.schemaplexai.context.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.impl.KnowledgeDocServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocServiceImplTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

    @Mock
    private MilvusSyncService milvusSyncService;

    @InjectMocks
    private KnowledgeDocServiceImpl knowledgeDocService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(knowledgeDocService, "baseMapper", knowledgeDocMapper);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SfKnowledgeDoc.class);
    }

    // ------------------------------------------------------------------
    // uploadAndVectorize
    // ------------------------------------------------------------------

    @Test
    void uploadAndVectorize_success_savesDoc() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Test Doc");
        doc.setFileName("test.pdf");
        when(knowledgeDocMapper.insert(any())).thenReturn(1);

        knowledgeDocService.uploadAndVectorize(doc);

        verify(knowledgeDocMapper).insert(any());
    }

    @Test
    void uploadAndVectorize_setsFieldsCorrectly() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Architecture Guide");
        doc.setFileName("arch.pdf");
        doc.setDocType("PDF");
        doc.setFileUrl("http://minio/bucket/arch.pdf");
        doc.setFileSize(1024L);
        when(knowledgeDocMapper.insert(any())).thenReturn(1);

        knowledgeDocService.uploadAndVectorize(doc);

        verify(knowledgeDocMapper).insert(doc);
    }

    // ------------------------------------------------------------------
    // removeById
    // ------------------------------------------------------------------

    @Test
    void removeById_deletesMilvusVectorsBeforeDbRemoval() {
        when(knowledgeDocMapper.deleteById(any(SfKnowledgeDoc.class))).thenReturn(1);

        boolean result = knowledgeDocService.removeById(1L);

        assertThat(result).isTrue();
        verify(milvusSyncService).deleteByDocId(1L);
        verify(knowledgeDocMapper).deleteById(any(SfKnowledgeDoc.class));
    }

    @Test
    void removeById_milvusDeleteFailure_stillRemovesDbRecord() {
        doThrow(new RuntimeException("Milvus connection failed")).when(milvusSyncService).deleteByDocId(1L);
        when(knowledgeDocMapper.deleteById(any(SfKnowledgeDoc.class))).thenReturn(1);

        boolean result = knowledgeDocService.removeById(1L);

        assertThat(result).isTrue();
        verify(milvusSyncService).deleteByDocId(1L);
        verify(knowledgeDocMapper).deleteById(any(SfKnowledgeDoc.class));
    }

    // ------------------------------------------------------------------
    // updateById
    // ------------------------------------------------------------------

    @Test
    void updateById_triggersReSyncOnSuccess() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Updated Title");
        when(knowledgeDocMapper.updateById(doc)).thenReturn(1);

        boolean result = knowledgeDocService.updateById(doc);

        assertThat(result).isTrue();
        verify(milvusSyncService).reSyncDoc(1L);
    }

    @Test
    void updateById_noReSyncWhenUpdateFails() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Updated Title");
        when(knowledgeDocMapper.updateById(doc)).thenReturn(0);

        boolean result = knowledgeDocService.updateById(doc);

        assertThat(result).isFalse();
        verify(milvusSyncService, never()).reSyncDoc(any());
    }

    @Test
    void updateById_milvusReSyncFailure_stillReturnsTrue() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Updated Title");
        when(knowledgeDocMapper.updateById(doc)).thenReturn(1);
        doThrow(new RuntimeException("Milvus sync failed")).when(milvusSyncService).reSyncDoc(1L);

        boolean result = knowledgeDocService.updateById(doc);

        assertThat(result).isTrue();
        verify(milvusSyncService).reSyncDoc(1L);
    }
}
