package com.schemaplexai.context.service;

import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.impl.KnowledgeDocServiceImpl;
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

    @InjectMocks
    private KnowledgeDocServiceImpl knowledgeDocService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(knowledgeDocService, "baseMapper", knowledgeDocMapper);
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
}
