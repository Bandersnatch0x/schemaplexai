package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.service.SpecTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecTemplateControllerTest {

    @Mock
    private SpecTemplateService specTemplateService;

    @InjectMocks
    private SpecTemplateController specTemplateController;

    private SfSpecTemplate template;
    private SfSpec spec;

    @BeforeEach
    void setUp() {
        template = new SfSpecTemplate();
        template.setId(1L);
        template.setName("Default PRD");
        template.setContent("template content");
        template.setCategory("PRD");

        spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("New Spec");
        spec.setType("PRD");
    }

    // ========== CRUD ==========

    @Test
    void create_returnsId() {
        when(specTemplateService.save(any())).thenReturn(true);

        Result<Long> result = specTemplateController.create(template);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(specTemplateService.updateById(any())).thenReturn(true);

        Result<Boolean> result = specTemplateController.update(1L, template);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(specTemplateService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = specTemplateController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(specTemplateService.getById(1L)).thenReturn(template);

        Result<SfSpecTemplate> result = specTemplateController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Default PRD");
    }

    @Test
    void get_notFound() {
        when(specTemplateService.getById(1L)).thenReturn(null);

        Result<SfSpecTemplate> result = specTemplateController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void page_returnsPageResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecTemplate> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(template));
        when(specTemplateService.page(any())).thenReturn(mpPage);

        PageParam pageParam = new PageParam();
        pageParam.setCurrent(1L);
        pageParam.setSize(10L);

        Result<PageResult<SfSpecTemplate>> result = specTemplateController.page(pageParam);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
    }

    // ========== Custom endpoints ==========

    @Test
    void applyTemplate_returnsSpec() {
        when(specTemplateService.applyTemplate(1L, null, "New Spec", "PRD")).thenReturn(spec);

        Result<SfSpec> result = specTemplateController.applyTemplate(1L, null, "New Spec", "PRD");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTitle()).isEqualTo("New Spec");
    }

    @Test
    void applyTemplate_withSpecId_returnsSpec() {
        when(specTemplateService.applyTemplate(1L, 2L, "Updated Spec", "PRD")).thenReturn(spec);

        Result<SfSpec> result = specTemplateController.applyTemplate(1L, 2L, "Updated Spec", "PRD");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void getDefaultTemplate_found() {
        when(specTemplateService.getDefaultTemplate("PRD")).thenReturn(Optional.of(template));

        Result<SfSpecTemplate> result = specTemplateController.getDefaultTemplate("PRD");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getCategory()).isEqualTo("PRD");
    }

    @Test
    void getDefaultTemplate_notFound() {
        when(specTemplateService.getDefaultTemplate("PRD")).thenReturn(Optional.empty());

        Result<SfSpecTemplate> result = specTemplateController.getDefaultTemplate("PRD");

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void listTemplatesByCategory_returnsList() {
        when(specTemplateService.listTemplatesByCategory("PRD")).thenReturn(List.of(template));

        Result<List<SfSpecTemplate>> result = specTemplateController.listTemplatesByCategory("PRD");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void cloneTemplate_returnsTemplate() {
        SfSpecTemplate cloned = new SfSpecTemplate();
        cloned.setId(2L);
        cloned.setName("Cloned Template");

        when(specTemplateService.cloneTemplate(1L, "Cloned Template")).thenReturn(cloned);

        Result<SfSpecTemplate> result = specTemplateController.cloneTemplate(1L, "Cloned Template");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Cloned Template");
    }
}
