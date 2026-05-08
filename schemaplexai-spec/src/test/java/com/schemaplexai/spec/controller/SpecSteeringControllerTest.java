package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecSteering;
import com.schemaplexai.spec.service.SpecSteeringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecSteeringControllerTest {

    @Mock
    private SpecSteeringService specSteeringService;

    @InjectMocks
    private SpecSteeringController specSteeringController;

    private SfSpecSteering steering;

    @BeforeEach
    void setUp() {
        steering = new SfSpecSteering();
        steering.setId(1L);
        steering.setSpecId(1L);
        steering.setDirection("enhance");
        steering.setConstraints("max 1000 words");
        steering.setAcceptanceCriteria("clear and concise");
    }

    // ========== CRUD ==========

    @Test
    void create_returnsId() {
        when(specSteeringService.save(any())).thenReturn(true);

        Result<Long> result = specSteeringController.create(steering);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(specSteeringService.updateById(any())).thenReturn(true);

        Result<Boolean> result = specSteeringController.update(1L, steering);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(specSteeringService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = specSteeringController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(specSteeringService.getById(1L)).thenReturn(steering);

        Result<SfSpecSteering> result = specSteeringController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getDirection()).isEqualTo("enhance");
    }

    @Test
    void get_notFound() {
        when(specSteeringService.getById(1L)).thenReturn(null);

        Result<SfSpecSteering> result = specSteeringController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void page_returnsPageResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecSteering> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(steering));
        when(specSteeringService.page(any())).thenReturn(mpPage);

        PageParam pageParam = new PageParam();
        pageParam.setCurrent(1L);
        pageParam.setSize(10L);

        Result<PageResult<SfSpecSteering>> result = specSteeringController.page(pageParam);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
    }

    // ========== Custom endpoints ==========

    @Test
    void evaluateSteeringRules_returnsMap() {
        Map<String, Boolean> evaluation = Map.of("length", true, "clarity", false);
        when(specSteeringService.evaluateSteeringRules(1L, "test content")).thenReturn(evaluation);

        Result<Map<String, Boolean>> result = specSteeringController.evaluateSteeringRules(1L, "test content");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get("length")).isTrue();
    }

    @Test
    void applySteering_returnsString() {
        when(specSteeringService.applySteering(1L, "original")).thenReturn("guided");

        Result<String> result = specSteeringController.applySteering(1L, "original");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("guided");
    }

    @Test
    void listActiveSteerings_returnsList() {
        when(specSteeringService.listActiveSteerings(1L)).thenReturn(List.of(steering));

        Result<List<SfSpecSteering>> result = specSteeringController.listActiveSteerings(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void validateSteeringConfig_returnsTrue() {
        when(specSteeringService.validateSteeringConfig(1L)).thenReturn(true);

        Result<Boolean> result = specSteeringController.validateSteeringConfig(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void validateSteeringConfig_returnsFalse() {
        when(specSteeringService.validateSteeringConfig(1L)).thenReturn(false);

        Result<Boolean> result = specSteeringController.validateSteeringConfig(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }
}
