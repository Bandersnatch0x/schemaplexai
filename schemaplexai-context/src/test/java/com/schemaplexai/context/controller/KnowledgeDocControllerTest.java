package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.service.KnowledgeDocService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocControllerTest {

    @Mock
    private KnowledgeDocService knowledgeDocService;

    @InjectMocks
    private KnowledgeDocController controller;

    @Test
    void create_success() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doNothing().when(knowledgeDocService).uploadAndVectorize(doc);
        Result<Long> result = controller.create(doc);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_success() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        when(knowledgeDocService.updateById(doc)).thenReturn(true);
        Result<Boolean> result = controller.update(1L, doc);
        assertThat(result.getData()).isTrue();
        assertThat(doc.getId()).isEqualTo(1L);
    }

    @Test
    void delete_success() {
        when(knowledgeDocService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = controller.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        when(knowledgeDocService.getById(1L)).thenReturn(doc);
        Result<SfKnowledgeDoc> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void get_notFound() {
        when(knowledgeDocService.getById(1L)).thenReturn(null);
        Result<SfKnowledgeDoc> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }
}
