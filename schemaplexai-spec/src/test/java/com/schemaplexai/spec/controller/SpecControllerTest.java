package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecService;
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
class SpecControllerTest {

    @Mock
    private SpecService specService;

    @InjectMocks
    private SpecController specController;

    private SfSpec spec;
    private SfSpecVersion version;

    @BeforeEach
    void setUp() {
        spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("Test Spec");
        spec.setType("PRD");
        spec.setStatus("draft");
        spec.setContent("content");

        version = new SfSpecVersion();
        version.setId(1L);
        version.setSpecId(1L);
        version.setVersion("1.0.0");
        version.setContent("content");
    }

    // ========== CRUD ==========

    @Test
    void create_returnsId() {
        when(specService.save(any())).thenReturn(true);

        Result<Long> result = specController.create(spec);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(specService.updateById(any())).thenReturn(true);

        Result<Boolean> result = specController.update(1L, spec);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(specService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = specController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(specService.getById(1L)).thenReturn(spec);

        Result<SfSpec> result = specController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTitle()).isEqualTo("Test Spec");
    }

    @Test
    void get_notFound() {
        when(specService.getById(1L)).thenReturn(null);

        Result<SfSpec> result = specController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void page_returnsPageResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpec> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(spec));
        when(specService.page(any())).thenReturn(mpPage);

        PageParam pageParam = new PageParam();
        pageParam.setCurrent(1L);
        pageParam.setSize(10L);

        Result<PageResult<SfSpec>> result = specController.page(pageParam);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
    }

    // ========== Custom endpoints ==========

    @Test
    void publishSpec_returnsVersion() {
        when(specService.publishSpec(1L)).thenReturn(version);

        Result<SfSpecVersion> result = specController.publishSpec(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void archiveSpec_returnsBoolean() {
        when(specService.archiveSpec(1L)).thenReturn(true);

        Result<Boolean> result = specController.archiveSpec(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void getLatestVersion_found() {
        when(specService.getLatestVersion(1L)).thenReturn(Optional.of(version));

        Result<SfSpecVersion> result = specController.getLatestVersion(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void getLatestVersion_notFound() {
        when(specService.getLatestVersion(1L)).thenReturn(Optional.empty());

        Result<SfSpecVersion> result = specController.getLatestVersion(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void compareVersions_returnsList() {
        when(specService.compareVersions(1L, "1.0.0", "1.1.0")).thenReturn(List.of(version));

        Result<List<SfSpecVersion>> result = specController.compareVersions(1L, "1.0.0", "1.1.0");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void createFromTemplate_returnsSpec() {
        when(specService.createFromTemplate(1L, "New Spec", "PRD")).thenReturn(spec);

        Result<SfSpec> result = specController.createFromTemplate(1L, "New Spec", "PRD");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTitle()).isEqualTo("Test Spec");
    }
}
