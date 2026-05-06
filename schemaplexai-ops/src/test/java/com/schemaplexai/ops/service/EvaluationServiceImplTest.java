package com.schemaplexai.ops.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.mapper.EvalTaskMapper;
import com.schemaplexai.ops.service.EvaluationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvalTaskMapper evalTaskMapper;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evaluationService, "baseMapper", evalTaskMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // runEvaluation
    // ------------------------------------------------------------------

    @Test
    void runEvaluation_notFound_throwsNotFound() {
        when(evalTaskMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> evaluationService.runEvaluation(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void runEvaluation_alreadyRunning_throwsParamError() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setStatus(1);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> evaluationService.runEvaluation(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void runEvaluation_pending_startsEvaluation() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setDatasetId(10L);
        task.setAgentId(20L);
        task.setStatus(0);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.runEvaluation(1L);

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(evalTaskMapper).updateById(task);
    }

    @Test
    void runEvaluation_nullStatus_startsEvaluation() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setStatus(null);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.runEvaluation(1L);

        assertThat(result.getStatus()).isEqualTo(1);
    }

    @Test
    void runEvaluation_completed_restartsEvaluation() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setStatus(2);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.runEvaluation(1L);

        assertThat(result.getStatus()).isEqualTo(1);
    }

    @Test
    void runEvaluation_failed_restartsEvaluation() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setStatus(3);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.runEvaluation(1L);

        assertThat(result.getStatus()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // getEvaluationResults
    // ------------------------------------------------------------------

    @Test
    void getEvaluationResults_notFound_throwsNotFound() {
        when(evalTaskMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> evaluationService.getEvaluationResults(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getEvaluationResults_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setTenantId("tenant-1");
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> evaluationService.getEvaluationResults(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void getEvaluationResults_sameTenant_returnsTask() {
        TenantContextHolder.setTenantId("tenant-1");
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setTenantId("tenant-1");
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.getEvaluationResults(1L);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getEvaluationResults_nullTenant_returnsTask() {
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setTenantId("tenant-1");
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        SfEvalTask result = evaluationService.getEvaluationResults(1L);

        assertThat(result).isEqualTo(task);
    }

    @Test
    void getEvaluationResults_nullTaskTenant_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-1");
        SfEvalTask task = new SfEvalTask();
        task.setId(1L);
        task.setTenantId(null);
        when(evalTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> evaluationService.getEvaluationResults(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    // ------------------------------------------------------------------
    // listByDataset
    // ------------------------------------------------------------------

    @Test
    void listByDataset_returnsTasks() {
        SfEvalTask task = new SfEvalTask();
        task.setDatasetId(10L);
        when(evalTaskMapper.selectList(any())).thenReturn(List.of(task));

        List<SfEvalTask> result = evaluationService.listByDataset(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void listByDataset_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(evalTaskMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfEvalTask> result = evaluationService.listByDataset(10L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // listByStatus
    // ------------------------------------------------------------------

    @Test
    void listByStatus_withStatus_filtersByStatus() {
        SfEvalTask task = new SfEvalTask();
        task.setStatus(2);
        when(evalTaskMapper.selectList(any())).thenReturn(List.of(task));

        List<SfEvalTask> result = evaluationService.listByStatus(2);

        assertThat(result).hasSize(1);
    }

    @Test
    void listByStatus_nullStatus_returnsAll() {
        when(evalTaskMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfEvalTask> result = evaluationService.listByStatus(null);

        assertThat(result).isEmpty();
    }

    @Test
    void listByStatus_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(evalTaskMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfEvalTask> result = evaluationService.listByStatus(1);

        assertThat(result).isEmpty();
    }
}
