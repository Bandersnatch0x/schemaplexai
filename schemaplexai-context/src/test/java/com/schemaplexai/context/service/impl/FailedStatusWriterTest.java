package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedStatusWriterTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

    @InjectMocks
    private FailedStatusWriter failedStatusWriter;

    @Test
    void markFailed_nullDocId_throwsParamErrorWithoutLookupOrUpdate() {
        assertThatThrownBy(() -> failedStatusWriter.markFailed(null, "Error"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verifyNoInteractions(knowledgeDocMapper);
    }

    @Test
    void markFailed_existingDoc_updatesStatusAndSyncStatus() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setStatus("PENDING");
        doc.setSyncStatus("PENDING");
        when(knowledgeDocMapper.selectById(1L)).thenReturn(doc);

        failedStatusWriter.markFailed(1L, "Error");

        verify(knowledgeDocMapper).updateById(argThat(d ->
                "FAILED".equals(d.getStatus()) && "FAILED".equals(d.getSyncStatus())));
    }

    @Test
    void markFailed_missingDoc_logsWarning() {
        when(knowledgeDocMapper.selectById(1L)).thenReturn(null);

        failedStatusWriter.markFailed(1L, "Error");

        verify(knowledgeDocMapper, never()).updateById(any());
    }
}
