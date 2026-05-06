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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecReviewServiceImplTest {

    @Mock
    private SfSpecMapper specMapper;

    @Mock
    private SfSpecReviewMapper specReviewMapper;

    @InjectMocks
    private SpecReviewServiceImpl specReviewService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specReviewService, "baseMapper", specReviewMapper);
    }

    // ------------------------------------------------------------------
    // submitReview
    // ------------------------------------------------------------------

    @Test
    void submitReview_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void submitReview_approved_updatesSpecStatusToApproved() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("DRAFT");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "APPROVED", "Looks good");

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getReviewerId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(spec.getStatus()).isEqualTo("APPROVED");
        verify(specMapper).updateById(spec);
    }

    @Test
    void submitReview_rejected_updatesSpecStatusToChangesRequested() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("DRAFT");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "REJECTED", "Needs work");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(spec.getStatus()).isEqualTo("CHANGES_REQUESTED");
        verify(specMapper).updateById(spec);
    }

    @Test
    void submitReview_changesRequested_updatesSpecStatus() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("DRAFT");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "changes_requested", "Fix X");

        assertThat(result.getStatus()).isEqualTo("changes_requested");
        assertThat(spec.getStatus()).isEqualTo("CHANGES_REQUESTED");
    }

    @Test
    void submitReview_otherStatus_doesNotUpdateSpec() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("DRAFT");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);

        specReviewService.submitReview(1L, 10L, "PENDING", "Reviewing");

        assertThat(spec.getStatus()).isEqualTo("DRAFT");
        verify(specMapper, never()).updateById(any());
    }

    @Test
    void submitReview_success_insertsReview() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specReviewMapper.insert(any())).thenReturn(1);

        SfSpecReview result = specReviewService.submitReview(1L, 10L, "APPROVED", "LGTM");

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("LGTM");
        verify(specReviewMapper).insert(any());
    }
}
