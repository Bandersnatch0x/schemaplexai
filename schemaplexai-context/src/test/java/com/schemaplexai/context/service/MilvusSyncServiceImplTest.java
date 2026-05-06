package com.schemaplexai.context.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.impl.MilvusSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilvusSyncServiceImplTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

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

        milvusSyncService.syncToMilvus(1L);

        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper).updateById(doc);
    }

    @Test
    void syncToMilvus_alreadySynced_stillUpdates() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setStatus("SYNCED");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        milvusSyncService.syncToMilvus(1L);

        assertThat(doc.getStatus()).isEqualTo("SYNCED");
        verify(knowledgeDocMapper).updateById(doc);
    }
}
