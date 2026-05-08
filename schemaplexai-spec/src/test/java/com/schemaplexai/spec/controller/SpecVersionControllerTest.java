package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecVersionControllerTest {

    @Mock
    private SpecVersionService specVersionService;

    @InjectMocks
    private SpecVersionController specVersionController;

    private SfSpecVersion version;

    @BeforeEach
    void setUp() {
        version = new SfSpecVersion();
        version.setId(1L);
        version.setSpecId(1L);
        version.setVersion("1.0.0");
        version.setContent("content");
        version.setChangeLog("init");
    }

    // ========== CRUD ==========

    @Test
    void create_returnsId() {
        when(specVersionService.save(any())).thenReturn(true);

        Result<Long> result = specVersionController.create(version);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(specVersionService.updateById(any())).thenReturn(true);

        Result<Boolean> result = specVersionController.update(1L, version);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(specVersionService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = specVersionController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(specVersionService.getById(1L)).thenReturn(version);

        Result<SfSpecVersion> result = specVersionController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void get_notFound() {
        when(specVersionService.getById(1L)).thenReturn(null);

        Result<SfSpecVersion> result = specVersionController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void page_returnsPageResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecVersion> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(version));
        when(specVersionService.page(any())).thenReturn(mpPage);

        PageParam pageParam = new PageParam();
        pageParam.setCurrent(1L);
        pageParam.setSize(10L);

        Result<PageResult<SfSpecVersion>> result = specVersionController.page(pageParam);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
    }

    // ========== Custom endpoints ==========

    @Test
    void diff_returnsResult() {
        SpecDiffResult diffResult = new SpecDiffResult();
        diffResult.setSpecId(1L);
        diffResult.setVersionAId(1L);
        diffResult.setVersionBId(2L);
        diffResult.setHunks(List.of());

        when(specVersionService.diff(1L, 2L)).thenReturn(diffResult);

        Result<SpecDiffResult> result = specVersionController.diff(1L, 2L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getSpecId()).isEqualTo(1L);
    }

    @Test
    void publish_returnsVersion() {
        when(specVersionService.createVersion(1L, "1.0.0", "content", "init")).thenReturn(version);

        Result<SfSpecVersion> result = specVersionController.publish(1L, "1.0.0", "content", "init");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getVersion()).isEqualTo("1.0.0");
    }
}
