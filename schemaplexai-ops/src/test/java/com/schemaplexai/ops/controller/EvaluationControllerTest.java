package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock
    private EvaluationService evaluationService;

    @InjectMocks
    private EvaluationController evaluationController;

    private SfEvalTask evalTask;

    @BeforeEach
    void setUp() {
        evalTask = new SfEvalTask();
        evalTask.setId(1L);
        evalTask.setDatasetId(10L);
        evalTask.setAgentId(20L);
        evalTask.setStatus(0);
    }

    @Test
    void create_returnsId() {
        when(evaluationService.save(any())).thenReturn(true);

        Result<Long> result = evaluationController.create(evalTask);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(evaluationService.updateById(any())).thenReturn(true);

        Result<Boolean> result = evaluationController.update(1L, evalTask);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(evaluationService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = evaluationController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(evaluationService.getById(1L)).thenReturn(evalTask);

        Result<SfEvalTask> result = evaluationController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getDatasetId()).isEqualTo(10L);
    }

    @Test
    void get_notFound() {
        when(evaluationService.getById(1L)).thenReturn(null);

        Result<SfEvalTask> result = evaluationController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void list_returnsEvalTasks() {
        when(evaluationService.list()).thenReturn(List.of(evalTask));

        Result<List<SfEvalTask>> result = evaluationController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void runEvaluation_returnsTask() {
        when(evaluationService.runEvaluation(1L)).thenReturn(evalTask);

        Result<SfEvalTask> result = evaluationController.runEvaluation(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void getEvaluationResults_returnsTask() {
        when(evaluationService.getEvaluationResults(1L)).thenReturn(evalTask);

        Result<SfEvalTask> result = evaluationController.getEvaluationResults(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void listByDataset_returnsTasks() {
        when(evaluationService.listByDataset(10L)).thenReturn(List.of(evalTask));

        Result<List<SfEvalTask>> result = evaluationController.listByDataset(10L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void listByStatus_returnsTasks() {
        when(evaluationService.listByStatus(0)).thenReturn(List.of(evalTask));

        Result<List<SfEvalTask>> result = evaluationController.listByStatus(0);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }
}
