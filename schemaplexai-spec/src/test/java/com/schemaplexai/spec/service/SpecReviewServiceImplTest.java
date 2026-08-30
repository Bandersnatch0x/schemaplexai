package com.schemaplexai.spec.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecReviewMapper;
import com.schemaplexai.spec.service.impl.SpecReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecReviewServiceImplTest {

    @Mock
    private SfSpecMapper specMapper;

    @Mock
    private SfSpecReviewMapper specReviewMapper;

    @Mock
    private SpecChangeTracker changeTracker;

    @InjectMocks
    private SpecReviewServiceImpl specReviewService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specReviewService, "baseMapper", specReviewMapper);
    }

    // ------------------------------------------------------------------
    // submitReview — parameter validation
    // ------------------------------------------------------------------

    @Test
    void submitReview_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specReviewService.submitReview(null, 10L, "APPROVED", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
        verify(specReviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_nullReviewerId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specReviewService.submitReview(1L, null, "APPROVED", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
        verify(specReviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_blankStatus_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, " ", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
        verify(specReviewMapper, never()).insert(any());
    }

    @Test
    void submitReview_unknownDecision_throwsParamError() {
        // §4.1 defines exactly three decisions; a fourth value must be a
        // client error, not a silently-passing branch.
        assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, "PENDING", "Reviewing"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
        verify(specReviewMapper, never()).insert(any());
        verify(specMapper, never()).updateById(any());
    }

    @Test
    void submitReview_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    // ------------------------------------------------------------------
    // submitReview — reviewable precondition (terminal states stay closed)
    // ------------------------------------------------------------------

    @Test
    void submitReview_terminalOrInactiveStatus_throwsForbidden() {
        for (String status : new String[]{"approved", "published", "archived", "rejected"}) {
            SfSpec spec = new SfSpec();
            spec.setId(1L);
            spec.setStatus(status);
            when(specMapper.selectById(1L)).thenReturn(spec);

            assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM"))
                    .as("status " + status)
                    .isInstanceOf(BaseException.class)
                    .extracting("code")
                    .isEqualTo(ResultCode.FORBIDDEN.getCode());
        }

        // A REJECTED flow genuinely ends: no review record, no status rewrite.
        verify(specReviewMapper, never()).insert(any());
        verify(specMapper, never()).updateById(any());
    }

    @Test
    void submitReview_inReviewStatus_isAllowed() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("in_review");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(spec.getStatus()).isEqualTo("approved");
    }

    // ------------------------------------------------------------------
    // submitReview — three independent branches (§4.1)
    // ------------------------------------------------------------------

    @Test
    void submitReview_approved_updatesSpecStatusToApproved() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "APPROVED", "Looks good");

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getReviewerId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(spec.getStatus()).isEqualTo("approved");
        verify(specMapper).updateById(spec);
        verify(changeTracker).recordUpdate(any(SfSpec.class), eq(spec), isNull());
    }

    @Test
    void submitReview_rejected_movesSpecToTerminalRejectedStatus() {
        // REQ-10: REJECTED is 结束流程 — its own terminal lifecycle state,
        // deliberately NOT folded into draft (changes requested).
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "REJECTED", "Not viable");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(spec.getStatus()).isEqualTo("rejected");
        verify(specMapper).updateById(spec);
        verify(changeTracker).recordUpdate(any(SfSpec.class), eq(spec), isNull());
    }

    @Test
    void submitReview_changesRequested_returnsSpecToDraft() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("in_review");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "changes_requested", "Fix X");

        // The stored decision is normalized to the §4.2 vocabulary.
        assertThat(result.getStatus()).isEqualTo("CHANGES_REQUESTED");
        assertThat(spec.getStatus()).isEqualTo("draft");
        verify(specMapper).updateById(spec);
    }

    @Test
    void submitReview_lowercaseApproved_isNormalized() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "approved", "LGTM");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(spec.getStatus()).isEqualTo("approved");
    }

    @Test
    void submitReview_zeroRowUpdate_throwsConflict() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(0); // a concurrent writer won the race

        assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(changeTracker, never()).recordUpdate(any(), any(), any());
    }

    @Test
    void submitReview_success_insertsReview() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM");

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("LGTM");
        verify(specReviewMapper).insert(any());
    }
}
