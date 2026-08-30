package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class QualityGateServiceImplTest {

    @Mock
    private QualityGateMapper qualityGateMapper;

    @Mock
    private QualityIssueMapper qualityIssueMapper;

    @Mock
    private QualityOrchestrator qualityOrchestrator;

    @InjectMocks
    private QualityGateServiceImpl qualityGateService;

    @BeforeAll
    static void initTableInfo() {
        // Enables LambdaQueryWrapper column resolution in pure unit tests
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SfQualityGate.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qualityGateService, "baseMapper", qualityGateMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullName_throwsParamError() {
        SfQualityGate gate = new SfQualityGate();

        assertThatThrownBy(() -> qualityGateService.save(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_blankName_throwsParamError() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("  ");

        assertThatThrownBy(() -> qualityGateService.save(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nameTooLong_throwsParamError() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("a".repeat(129));

        assertThatThrownBy(() -> qualityGateService.save(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success_setsDefaultStatus() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Security Gate");
        when(qualityGateMapper.insert(any())).thenReturn(1);

        boolean result = qualityGateService.save(gate);

        assertThat(result).isTrue();
        assertThat(gate.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void save_whenInsertAffectsNoRows_throwsInternalError() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Security Gate");
        when(qualityGateMapper.insert(any())).thenReturn(0);

        assertThatThrownBy(() -> qualityGateService.save(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }

    // ------------------------------------------------------------------
    // updateById
    // ------------------------------------------------------------------

    @Test
    void updateById_nullId_throwsParamError() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Updated");

        assertThatThrownBy(() -> qualityGateService.updateById(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateById_notFound_throwsNotFound() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setName("Updated");
        when(qualityGateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.updateById(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateById_success_updatesName() {
        SfQualityGate existing = new SfQualityGate();
        existing.setId(1L);
        existing.setName("Old");
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setName("Updated");
        when(qualityGateMapper.selectById(1L)).thenReturn(existing);
        when(qualityGateMapper.updateById(any())).thenReturn(1);

        boolean result = qualityGateService.updateById(gate);

        assertThat(result).isTrue();
    }

    @Test
    void updateById_whenUpdateAffectsNoRows_throwsNotFound() {
        SfQualityGate existing = new SfQualityGate();
        existing.setId(1L);
        existing.setName("Old");
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setName("Updated");
        when(qualityGateMapper.selectById(1L)).thenReturn(existing);
        when(qualityGateMapper.updateById(any())).thenReturn(0);

        assertThatThrownBy(() -> qualityGateService.updateById(gate))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    // ------------------------------------------------------------------
    // activateGate
    // ------------------------------------------------------------------

    @Test
    void activateGate_notFound_throwsNotFound() {
        when(qualityGateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.activateGate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void activateGate_deprecated_throwsParamError() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setStatus("DEPRECATED");
        when(qualityGateMapper.selectById(1L)).thenReturn(gate);

        assertThatThrownBy(() -> qualityGateService.activateGate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void activateGate_success_setsActive() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setStatus("INACTIVE");
        when(qualityGateMapper.selectById(1L)).thenReturn(gate);

        qualityGateService.activateGate(1L);

        assertThat(gate.getStatus()).isEqualTo("ACTIVE");
        verify(qualityGateMapper).updateById(gate);
    }

    // ------------------------------------------------------------------
    // deactivateGate
    // ------------------------------------------------------------------

    @Test
    void deactivateGate_notFound_throwsNotFound() {
        when(qualityGateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.deactivateGate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deactivateGate_success_setsInactive() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        gate.setStatus("ACTIVE");
        when(qualityGateMapper.selectById(1L)).thenReturn(gate);

        qualityGateService.deactivateGate(1L);

        assertThat(gate.getStatus()).isEqualTo("INACTIVE");
        verify(qualityGateMapper).updateById(gate);
    }

    // ------------------------------------------------------------------
    // deprecateGate
    // ------------------------------------------------------------------

    @Test
    void deprecateGate_notFound_throwsNotFound() {
        when(qualityGateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.deprecateGate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deprecateGate_success_setsDeprecated() {
        SfQualityGate gate = new SfQualityGate();
        gate.setId(1L);
        when(qualityGateMapper.selectById(1L)).thenReturn(gate);

        qualityGateService.deprecateGate(1L);

        assertThat(gate.getStatus()).isEqualTo("DEPRECATED");
        verify(qualityGateMapper).updateById(gate);
    }

    // ------------------------------------------------------------------
    // evaluateGate
    // ------------------------------------------------------------------

    @Test
    void evaluateGate_notFound_throwsNotFound() {
        when(qualityGateMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.evaluateGate(1L, "Security"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void evaluateGate_success_returnsReport() {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Security");
        gate.setStatus("ACTIVE");
        when(qualityGateMapper.selectOne(any())).thenReturn(gate);
        QualityReport report = new QualityReport(1L, true, List.of());
        when(qualityOrchestrator.evaluate(any(), any())).thenReturn(report);

        QualityReport result = qualityGateService.evaluateGate(1L, "Security");

        assertThat(result.isAllPassed()).isTrue();
    }

    @Test
    void evaluateGate_filtersByActiveStatus() {
        when(qualityGateMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> qualityGateService.evaluateGate(1L, "Security"))
                .isInstanceOf(BaseException.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SfQualityGate>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(qualityGateMapper).selectOne(captor.capture());
        LambdaQueryWrapper<SfQualityGate> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains("Security", "ACTIVE");
    }

    // ------------------------------------------------------------------
    // listActiveGates
    // ------------------------------------------------------------------

    @Test
    void listActiveGates_returnsActiveGates() {
        SfQualityGate gate = new SfQualityGate();
        gate.setStatus("ACTIVE");
        when(qualityGateMapper.selectList(any())).thenReturn(List.of(gate));

        List<SfQualityGate> result = qualityGateService.listActiveGates();

        assertThat(result).hasSize(1);
    }

    @Test
    void listActiveGates_filtersByActiveStatus() {
        when(qualityGateMapper.selectList(any())).thenReturn(Collections.emptyList());

        qualityGateService.listActiveGates();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SfQualityGate>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(qualityGateMapper).selectList(captor.capture());
        LambdaQueryWrapper<SfQualityGate> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .containsExactly("ACTIVE");
    }

    // ------------------------------------------------------------------
    // getGateSummary
    // ------------------------------------------------------------------

    @Test
    void getGateSummary_returnsCorrectCounts() {
        SfQualityIssue i1 = new SfQualityIssue();
        i1.setSeverity("CRITICAL");
        i1.setStatus("OPEN");
        SfQualityIssue i2 = new SfQualityIssue();
        i2.setSeverity("HIGH");
        i2.setStatus("RESOLVED");
        when(qualityIssueMapper.selectList(any())).thenReturn(List.of(i1, i2));

        QualityGateServiceImpl.GateSummary result = qualityGateService.getGateSummary(1L);

        assertThat(result.totalIssues()).isEqualTo(2);
        assertThat(result.openIssues()).isEqualTo(1);
        assertThat(result.criticalCount()).isEqualTo(1);
        assertThat(result.highCount()).isEqualTo(1);
    }

    @Test
    void getGateSummary_noIssues_returnsZeroCounts() {
        when(qualityIssueMapper.selectList(any())).thenReturn(Collections.emptyList());

        QualityGateServiceImpl.GateSummary result = qualityGateService.getGateSummary(1L);

        assertThat(result.totalIssues()).isEqualTo(0);
    }
}
