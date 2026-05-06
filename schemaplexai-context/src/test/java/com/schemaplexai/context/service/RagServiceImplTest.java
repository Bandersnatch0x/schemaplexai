package com.schemaplexai.context.service;

import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.impl.RagServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

    @InjectMocks
    private RagServiceImpl ragService;

    // ------------------------------------------------------------------
    // retrieve - null/blank query
    // ------------------------------------------------------------------

    @Test
    void retrieve_nullQuery_returnsEmptyList() {
        List<String> result = ragService.retrieve(null, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void retrieve_blankQuery_returnsEmptyList() {
        List<String> result = ragService.retrieve("   ", 5);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // retrieve - keyword matching
    // ------------------------------------------------------------------

    @Test
    void retrieve_noDocs_returnsEmptyList() {
        when(knowledgeDocMapper.selectList(null)).thenReturn(Collections.emptyList());

        List<String> result = ragService.retrieve("test", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void retrieve_matchesByTitle_returnsDoc() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Architecture Guide");
        doc.setFileName("arch.pdf");
        doc.setDocType("PDF");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc));

        List<String> result = ragService.retrieve("architecture", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("Architecture Guide | arch.pdf");
    }

    @Test
    void retrieve_matchesByFileName_returnsDoc() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Guide");
        doc.setFileName("readme.md");
        doc.setDocType("MARKDOWN");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc));

        List<String> result = ragService.retrieve("readme", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("Guide | readme.md");
    }

    @Test
    void retrieve_matchesByDocType_returnsDoc() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Spec");
        doc.setFileName("spec.yaml");
        doc.setDocType("YAML");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc));

        List<String> result = ragService.retrieve("yaml", 5);

        assertThat(result).hasSize(1);
    }

    @Test
    void retrieve_noMatch_returnsEmptyList() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("Architecture");
        doc.setFileName("arch.pdf");
        doc.setDocType("PDF");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc));

        List<String> result = ragService.retrieve("nonexistent", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void retrieve_respectsTopK_limit() {
        SfKnowledgeDoc doc1 = new SfKnowledgeDoc();
        doc1.setTitle("Doc One");
        doc1.setFileName("one.pdf");
        SfKnowledgeDoc doc2 = new SfKnowledgeDoc();
        doc2.setTitle("Doc Two");
        doc2.setFileName("two.pdf");
        SfKnowledgeDoc doc3 = new SfKnowledgeDoc();
        doc3.setTitle("Doc Three");
        doc3.setFileName("three.pdf");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc1, doc2, doc3));

        List<String> result = ragService.retrieve("doc", 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void retrieve_caseInsensitive_match() {
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTitle("UPPERCASE GUIDE");
        doc.setFileName("guide.pdf");
        when(knowledgeDocMapper.selectList(null)).thenReturn(List.of(doc));

        List<String> result = ragService.retrieve("uppercase", 5);

        assertThat(result).hasSize(1);
    }
}
