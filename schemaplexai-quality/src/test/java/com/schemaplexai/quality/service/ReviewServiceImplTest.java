package com.schemaplexai.quality.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.mapper.ReviewRecordMapper;
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
class ReviewServiceImplTest {

    @Mock
    private ReviewRecordMapper reviewRecordMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reviewService, "baseMapper", reviewRecordMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullResourceType_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setResourceId(1L);

        assertThatThrownBy(() -> reviewService.save(record))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullResourceId_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setResourceType("SPEC");

        assertThatThrownBy(() -> reviewService.save(record))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success_setsDefaultStatus() {
        SfReviewRecord record = new SfReviewRecord();
        record.setResourceType("SPEC");
        record.setResourceId(1L);
        when(reviewRecordMapper.insert(any())).thenReturn(1);

        boolean result = reviewService.save(record);

        assertThat(result).isTrue();
        assertThat(record.getStatus()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // assignReview
    // ------------------------------------------------------------------

    @Test
    void assignReview_nullReviewerId_throwsParamError() {
        assertThatThrownBy(() -> reviewService.assignReview(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void assignReview_notFound_throwsNotFound() {
        when(reviewRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.assignReview(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void assignReview_alreadyCompleted_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(3);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.assignReview(1L, 10L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void assignReview_success_setsAssigned() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(0);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.assignReview(1L, 10L);

        assertThat(record.getReviewerId()).isEqualTo(10L);
        assertThat(record.getStatus()).isEqualTo(1);
        verify(reviewRecordMapper).updateById(record);
    }

    // ------------------------------------------------------------------
    // startReview
    // ------------------------------------------------------------------

    @Test
    void startReview_notFound_throwsNotFound() {
        when(reviewRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.startReview(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void startReview_noReviewer_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(0);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.startReview(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void startReview_alreadyInReview_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setReviewerId(10L);
        record.setStatus(2);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.startReview(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void startReview_alreadyCompleted_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setReviewerId(10L);
        record.setStatus(3);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.startReview(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void startReview_success_setsInReview() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setReviewerId(10L);
        record.setStatus(1);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.startReview(1L);

        assertThat(record.getStatus()).isEqualTo(2);
        verify(reviewRecordMapper).updateById(record);
    }

    // ------------------------------------------------------------------
    // approveReview
    // ------------------------------------------------------------------

    @Test
    void approveReview_notFound_throwsNotFound() {
        when(reviewRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.approveReview(1L, "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void approveReview_alreadyApproved_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(3);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.approveReview(1L, "LGTM"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void approveReview_success_appendsComment() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(2);
        record.setComment("Initial");
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.approveReview(1L, "Looks good");

        assertThat(record.getStatus()).isEqualTo(3);
        assertThat(record.getComment()).contains("APPROVED: Looks good");
    }

    @Test
    void approveReview_nullComment_doesNotAppend() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(2);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.approveReview(1L, null);

        assertThat(record.getComment()).isNull();
    }

    // ------------------------------------------------------------------
    // rejectReview
    // ------------------------------------------------------------------

    @Test
    void rejectReview_notFound_throwsNotFound() {
        when(reviewRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.rejectReview(1L, "Bad design"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void rejectReview_alreadyRejected_throwsParamError() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(4);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> reviewService.rejectReview(1L, "Bad"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void rejectReview_success_setsRejected() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(2);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.rejectReview(1L, "Needs work");

        assertThat(record.getStatus()).isEqualTo(4);
    }

    // ------------------------------------------------------------------
    // requestChanges
    // ------------------------------------------------------------------

    @Test
    void requestChanges_notFound_throwsNotFound() {
        when(reviewRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.requestChanges(1L, "Fix typo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void requestChanges_success_setsChangesRequested() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        record.setStatus(2);
        when(reviewRecordMapper.selectById(1L)).thenReturn(record);

        reviewService.requestChanges(1L, "Fix issues");

        assertThat(record.getStatus()).isEqualTo(5);
    }

    // ------------------------------------------------------------------
    // listPendingReviewsForReviewer
    // ------------------------------------------------------------------

    @Test
    void listPendingReviewsForReviewer_returnsAssignedAndInReview() {
        SfReviewRecord record = new SfReviewRecord();
        record.setReviewerId(10L);
        when(reviewRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<SfReviewRecord> result = reviewService.listPendingReviewsForReviewer(10L);

        assertThat(result).hasSize(1);
    }

    // ------------------------------------------------------------------
    // listReviewsForResource
    // ------------------------------------------------------------------

    @Test
    void listReviewsForResource_returnsReviews() {
        when(reviewRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfReviewRecord> result = reviewService.listReviewsForResource("SPEC", 1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // getLatestReviewForResource
    // ------------------------------------------------------------------

    @Test
    void getLatestReviewForResource_returnsFirstRecord() {
        SfReviewRecord record = new SfReviewRecord();
        when(reviewRecordMapper.selectList(any())).thenReturn(List.of(record));

        SfReviewRecord result = reviewService.getLatestReviewForResource("SPEC", 1L);

        assertThat(result).isEqualTo(record);
    }

    @Test
    void getLatestReviewForResource_noReviews_returnsNull() {
        when(reviewRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        SfReviewRecord result = reviewService.getLatestReviewForResource("SPEC", 1L);

        assertThat(result).isNull();
    }

    // ------------------------------------------------------------------
    // hasApprovedReview
    // ------------------------------------------------------------------

    @Test
    void hasApprovedReview_true_whenCountGreaterThanZero() {
        when(reviewRecordMapper.selectCount(any())).thenReturn(1L);

        boolean result = reviewService.hasApprovedReview("SPEC", 1L);

        assertThat(result).isTrue();
    }

    @Test
    void hasApprovedReview_false_whenCountIsZero() {
        when(reviewRecordMapper.selectCount(any())).thenReturn(0L);

        boolean result = reviewService.hasApprovedReview("SPEC", 1L);

        assertThat(result).isFalse();
    }
}
