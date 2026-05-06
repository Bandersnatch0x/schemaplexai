package com.schemaplexai.quality.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
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
class QualityIssueServiceImplTest {

    @Mock
    private QualityIssueMapper qualityIssueMapper;

    @InjectMocks
    private QualityIssueServiceImpl qualityIssueService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qualityIssueService, "baseMapper", qualityIssueMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullExecutionId_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setIssueType("BUG");

        assertThatThrownBy(() -> qualityIssueService.save(issue))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullIssueType_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setExecutionId(1L);

        assertThatThrownBy(() -> qualityIssueService.save(issue))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_invalidSeverity_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setExecutionId(1L);
        issue.setIssueType("BUG");
        issue.setSeverity("INVALID");

        assertThatThrownBy(() -> qualityIssueService.save(issue))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_validSeverity_upperCasesValue() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setExecutionId(1L);
        issue.setIssueType("BUG");
        issue.setSeverity("critical");
        when(qualityIssueMapper.insert(any())).thenReturn(1);

        qualityIssueService.save(issue);

        assertThat(issue.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void save_success_setsDefaultStatus() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setExecutionId(1L);
        issue.setIssueType("BUG");
        when(qualityIssueMapper.insert(any())).thenReturn(1);

        boolean result = qualityIssueService.save(issue);

        assertThat(result).isTrue();
        assertThat(issue.getStatus()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // listOpenIssuesByExecution
    // ------------------------------------------------------------------

    @Test
    void listOpenIssuesByExecution_returnsOpenIssues() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setStatus(0);
        when(qualityIssueMapper.selectList(any())).thenReturn(List.of(issue));

        List<SfQualityIssue> result = qualityIssueService.listOpenIssuesByExecution(1L);

        assertThat(result).hasSize(1);
    }

    // ------------------------------------------------------------------
    // listIssuesByExecution
    // ------------------------------------------------------------------

    @Test
    void listIssuesByExecution_returnsIssuesOrderedBySeverity() {
        when(qualityIssueMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfQualityIssue> result = qualityIssueService.listIssuesByExecution(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // resolveIssue
    // ------------------------------------------------------------------

    @Test
    void resolveIssue_notFound_throwsNotFound() {
        when(qualityIssueMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityIssueService.resolveIssue(1L, "Fixed"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void resolveIssue_alreadyResolved_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(2);
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        assertThatThrownBy(() -> qualityIssueService.resolveIssue(1L, "Fixed"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void resolveIssue_success_setsResolvedAndAppendsNotes() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(0);
        issue.setDescription("Original desc");
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        qualityIssueService.resolveIssue(1L, "Fixed in v2");

        assertThat(issue.getStatus()).isEqualTo(2);
        assertThat(issue.getDescription()).contains("[Resolution: Fixed in v2]");
        verify(qualityIssueMapper).updateById(issue);
    }

    @Test
    void resolveIssue_nullNotes_doesNotAppend() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(1);
        issue.setDescription("Original");
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        qualityIssueService.resolveIssue(1L, null);

        assertThat(issue.getDescription()).isEqualTo("Original");
    }

    // ------------------------------------------------------------------
    // markAsWontFix
    // ------------------------------------------------------------------

    @Test
    void markAsWontFix_notFound_throwsNotFound() {
        when(qualityIssueMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityIssueService.markAsWontFix(1L, "Not applicable"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void markAsWontFix_alreadyResolved_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(2);
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        assertThatThrownBy(() -> qualityIssueService.markAsWontFix(1L, "Not applicable"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void markAsWontFix_success_setsStatusAndAppendsReason() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(0);
        issue.setDescription("Desc");
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        qualityIssueService.markAsWontFix(1L, "Out of scope");

        assertThat(issue.getStatus()).isEqualTo(4);
        assertThat(issue.getDescription()).contains("[Won't Fix: Out of scope]");
    }

    // ------------------------------------------------------------------
    // reopenIssue
    // ------------------------------------------------------------------

    @Test
    void reopenIssue_notFound_throwsNotFound() {
        when(qualityIssueMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> qualityIssueService.reopenIssue(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void reopenIssue_alreadyOpen_throwsParamError() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(0);
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        assertThatThrownBy(() -> qualityIssueService.reopenIssue(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void reopenIssue_success_setsOpen() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        issue.setStatus(2);
        when(qualityIssueMapper.selectById(1L)).thenReturn(issue);

        qualityIssueService.reopenIssue(1L);

        assertThat(issue.getStatus()).isEqualTo(0);
        verify(qualityIssueMapper).updateById(issue);
    }

    // ------------------------------------------------------------------
    // getSeverityDistribution
    // ------------------------------------------------------------------

    @Test
    void getSeverityDistribution_returnsCorrectCounts() {
        SfQualityIssue i1 = new SfQualityIssue();
        i1.setSeverity("CRITICAL");
        SfQualityIssue i2 = new SfQualityIssue();
        i2.setSeverity("HIGH");
        SfQualityIssue i3 = new SfQualityIssue();
        i3.setSeverity("CRITICAL");
        when(qualityIssueMapper.selectList(any())).thenReturn(List.of(i1, i2, i3));

        QualityIssueServiceImpl.SeverityDistribution result = qualityIssueService.getSeverityDistribution(1L);

        assertThat(result.criticalCount()).isEqualTo(2);
        assertThat(result.highCount()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(3);
    }

    @Test
    void getSeverityDistribution_nullSeverity_treatsAsUnknown() {
        SfQualityIssue i1 = new SfQualityIssue();
        i1.setSeverity(null);
        when(qualityIssueMapper.selectList(any())).thenReturn(List.of(i1));

        QualityIssueServiceImpl.SeverityDistribution result = qualityIssueService.getSeverityDistribution(1L);

        assertThat(result.totalCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // bulkUpdateStatusBySeverity
    // ------------------------------------------------------------------

    @Test
    void bulkUpdateStatusBySeverity_updatesMatchingIssues() {
        SfQualityIssue i1 = new SfQualityIssue();
        i1.setId(1L);
        i1.setSeverity("HIGH");
        i1.setStatus(0);
        SfQualityIssue i2 = new SfQualityIssue();
        i2.setId(2L);
        i2.setSeverity("HIGH");
        i2.setStatus(0);
        when(qualityIssueMapper.selectList(any())).thenReturn(List.of(i1, i2));

        int result = qualityIssueService.bulkUpdateStatusBySeverity(1L, "HIGH", 2);

        assertThat(result).isEqualTo(2);
        assertThat(i1.getStatus()).isEqualTo(2);
        assertThat(i2.getStatus()).isEqualTo(2);
        verify(qualityIssueMapper, times(2)).updateById(any());
    }

    @Test
    void bulkUpdateStatusBySeverity_noMatches_returnsZero() {
        when(qualityIssueMapper.selectList(any())).thenReturn(Collections.emptyList());

        int result = qualityIssueService.bulkUpdateStatusBySeverity(1L, "HIGH", 2);

        assertThat(result).isEqualTo(0);
    }
}
