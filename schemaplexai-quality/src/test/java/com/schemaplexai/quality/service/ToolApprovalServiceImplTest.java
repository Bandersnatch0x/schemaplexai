package com.schemaplexai.quality.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfToolApprovalAmendment;
import com.schemaplexai.quality.mapper.ToolApprovalAmendmentMapper;
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
class ToolApprovalServiceImplTest {

    @Mock
    private ToolApprovalAmendmentMapper toolApprovalMapper;

    @InjectMocks
    private ToolApprovalServiceImpl toolApprovalService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(toolApprovalService, "baseMapper", toolApprovalMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullToolName_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();

        assertThatThrownBy(() -> toolApprovalService.save(approval))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_thresholdTooLow_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setToolName("TestTool");
        approval.setApprovalThreshold(0);

        assertThatThrownBy(() -> toolApprovalService.save(approval))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success_setsDefaults() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setToolName("TestTool");
        when(toolApprovalMapper.insert(any())).thenReturn(1);

        boolean result = toolApprovalService.save(approval);

        assertThat(result).isTrue();
        assertThat(approval.getStatus()).isEqualTo(0);
        assertThat(approval.getCurrentCount()).isEqualTo(0);
        assertThat(approval.getApprovalThreshold()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // submitApprovalVote
    // ------------------------------------------------------------------

    @Test
    void submitApprovalVote_nullVoterId_throwsParamError() {
        assertThatThrownBy(() -> toolApprovalService.submitApprovalVote(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void submitApprovalVote_notFound_throwsNotFound() {
        when(toolApprovalMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> toolApprovalService.submitApprovalVote(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void submitApprovalVote_alreadyApproved_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(1);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        assertThatThrownBy(() -> toolApprovalService.submitApprovalVote(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void submitApprovalVote_rejected_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(2);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        assertThatThrownBy(() -> toolApprovalService.submitApprovalVote(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void submitApprovalVote_reachesThreshold_setsApproved() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(0);
        approval.setCurrentCount(1);
        approval.setApprovalThreshold(2);
        approval.setToolName("ToolA");
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        toolApprovalService.submitApprovalVote(1L, 10L);

        assertThat(approval.getStatus()).isEqualTo(1);
        assertThat(approval.getCurrentCount()).isEqualTo(2);
        verify(toolApprovalMapper).updateById(approval);
    }

    @Test
    void submitApprovalVote_belowThreshold_staysPending() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(0);
        approval.setCurrentCount(0);
        approval.setApprovalThreshold(3);
        approval.setToolName("ToolA");
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        toolApprovalService.submitApprovalVote(1L, 10L);

        assertThat(approval.getStatus()).isEqualTo(0);
        assertThat(approval.getCurrentCount()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // rejectApproval
    // ------------------------------------------------------------------

    @Test
    void rejectApproval_notFound_throwsNotFound() {
        when(toolApprovalMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> toolApprovalService.rejectApproval(1L, "Unsafe"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void rejectApproval_alreadyApproved_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(1);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        assertThatThrownBy(() -> toolApprovalService.rejectApproval(1L, "Unsafe"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void rejectApproval_success_setsRejected() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(0);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        toolApprovalService.rejectApproval(1L, "Not safe");

        assertThat(approval.getStatus()).isEqualTo(2);
        verify(toolApprovalMapper).updateById(approval);
    }

    // ------------------------------------------------------------------
    // revokeApproval
    // ------------------------------------------------------------------

    @Test
    void revokeApproval_notFound_throwsNotFound() {
        when(toolApprovalMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> toolApprovalService.revokeApproval(1L, "Security concern"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void revokeApproval_notApproved_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(0);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        assertThatThrownBy(() -> toolApprovalService.revokeApproval(1L, "Concern"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void revokeApproval_success_setsRevokedAndResetsCount() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(1);
        approval.setCurrentCount(5);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        toolApprovalService.revokeApproval(1L, "Security issue");

        assertThat(approval.getStatus()).isEqualTo(3);
        assertThat(approval.getCurrentCount()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // resetApproval
    // ------------------------------------------------------------------

    @Test
    void resetApproval_notFound_throwsNotFound() {
        when(toolApprovalMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> toolApprovalService.resetApproval(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void resetApproval_alreadyPending_throwsParamError() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(0);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        assertThatThrownBy(() -> toolApprovalService.resetApproval(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void resetApproval_success_setsPendingAndResetsCount() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setId(1L);
        approval.setStatus(2);
        approval.setCurrentCount(3);
        when(toolApprovalMapper.selectById(1L)).thenReturn(approval);

        toolApprovalService.resetApproval(1L);

        assertThat(approval.getStatus()).isEqualTo(0);
        assertThat(approval.getCurrentCount()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // findByToolName
    // ------------------------------------------------------------------

    @Test
    void findByToolName_returnsLatestApproval() {
        SfToolApprovalAmendment approval = new SfToolApprovalAmendment();
        approval.setToolName("ToolA");
        when(toolApprovalMapper.selectList(any())).thenReturn(List.of(approval));

        SfToolApprovalAmendment result = toolApprovalService.findByToolName("ToolA");

        assertThat(result.getToolName()).isEqualTo("ToolA");
    }

    @Test
    void findByToolName_noResults_returnsNull() {
        when(toolApprovalMapper.selectList(any())).thenReturn(Collections.emptyList());

        SfToolApprovalAmendment result = toolApprovalService.findByToolName("ToolA");

        assertThat(result).isNull();
    }

    // ------------------------------------------------------------------
    // listPendingApprovals / listApprovedTools
    // ------------------------------------------------------------------

    @Test
    void listPendingApprovals_returnsPending() {
        when(toolApprovalMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfToolApprovalAmendment> result = toolApprovalService.listPendingApprovals();

        assertThat(result).isEmpty();
    }

    @Test
    void listApprovedTools_returnsApproved() {
        when(toolApprovalMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfToolApprovalAmendment> result = toolApprovalService.listApprovedTools();

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // isToolApproved
    // ------------------------------------------------------------------

    @Test
    void isToolApproved_true_whenCountGreaterThanZero() {
        when(toolApprovalMapper.selectCount(any())).thenReturn(1L);

        boolean result = toolApprovalService.isToolApproved("ToolA");

        assertThat(result).isTrue();
    }

    @Test
    void isToolApproved_false_whenCountIsZero() {
        when(toolApprovalMapper.selectCount(any())).thenReturn(0L);

        boolean result = toolApprovalService.isToolApproved("ToolA");

        assertThat(result).isFalse();
    }

    // ------------------------------------------------------------------
    // getApprovalSummary
    // ------------------------------------------------------------------

    @Test
    void getApprovalSummary_returnsCorrectCounts() {
        when(toolApprovalMapper.selectCount(any())).thenReturn(2L);

        ToolApprovalServiceImpl.ApprovalSummary result = toolApprovalService.getApprovalSummary();

        assertThat(result.pendingCount()).isEqualTo(2);
        assertThat(result.approvedCount()).isEqualTo(2);
        assertThat(result.rejectedCount()).isEqualTo(2);
        assertThat(result.revokedCount()).isEqualTo(2);
    }
}
