package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.service.QualityGateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityGateControllerTest {

    @Mock
    private QualityGateService qualityGateService;

    @InjectMocks
    private QualityGateController qualityGateController;

    @Test
    void create_returnsId() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);

        Result<Long> result = qualityGateController.create(gate);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(qualityGateService).save(gate);
    }

    @Test
    void update_returnsBoolean() {
        SfQualityGate gate = new SfQualityGate();
        when(qualityGateService.updateById(any())).thenReturn(true);

        Result<Boolean> result = qualityGateController.update(1L, gate);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(gate.getId()).isEqualTo(1L);
        verify(qualityGateService).updateById(gate);
    }

    @Test
    void update_returnsFalse_whenServiceFails() {
        SfQualityGate gate = new SfQualityGate();
        when(qualityGateService.updateById(any())).thenReturn(false);

        Result<Boolean> result = qualityGateController.update(1L, gate);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void delete_returnsBoolean() {
        when(qualityGateService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = qualityGateController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(qualityGateService).removeById(1L);
    }

    @Test
    void delete_returnsFalse_whenServiceFails() {
        when(qualityGateService.removeById(1L)).thenReturn(false);

        Result<Boolean> result = qualityGateController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void get_found() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        when(qualityGateService.getById(1L)).thenReturn(gate);

        Result<SfQualityGate> result = qualityGateController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(gate);
    }

    @Test
    void get_notFound() {
        when(qualityGateService.getById(1L)).thenReturn(null);

        Result<SfQualityGate> result = qualityGateController.get(1L);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void list_returnsGates() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        when(qualityGateService.list()).thenReturn(List.of(gate));

        Result<List<SfQualityGate>> result = qualityGateController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void list_returnsEmptyList() {
        when(qualityGateService.list()).thenReturn(Collections.emptyList());

        Result<List<SfQualityGate>> result = qualityGateController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
