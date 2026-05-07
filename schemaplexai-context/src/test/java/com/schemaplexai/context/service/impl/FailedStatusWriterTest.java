package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedStatusWriterTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

    @InjectMocks
    private FailedStatusWriter failedStatusWriter;

    @Test
    void markFailed_existingDoc_updatesStatus() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        failedStatusWriter.markFailed(1L, "Error");

        verify(knowledgeDocMapper).updateById(argThat(d -> "FAILED".equals(d.getStatus())));
    }

    @Test
    void markFailed_missingDoc_logsWarning() {
        when(knowledgeDocMapper.selectById(1L)).thenReturn(null);

        failedStatusWriter.markFailed(1L, "Error");

        verify(knowledgeDocMapper, never()).updateById(any());
    }
}
