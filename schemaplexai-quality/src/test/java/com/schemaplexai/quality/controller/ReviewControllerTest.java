package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.service.ReviewService;
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
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @Test
    void create_returnsId() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);

        Result<Long> result = reviewController.create(record);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(reviewService).save(record);
    }

    @Test
    void update_returnsBoolean() {
        SfReviewRecord record = new SfReviewRecord();
        when(reviewService.updateById(any())).thenReturn(true);

        Result<Boolean> result = reviewController.update(1L, record);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(record.getId()).isEqualTo(1L);
        verify(reviewService).updateById(record);
    }

    @Test
    void update_returnsFalse_whenServiceFails() {
        SfReviewRecord record = new SfReviewRecord();
        when(reviewService.updateById(any())).thenReturn(false);

        Result<Boolean> result = reviewController.update(1L, record);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void delete_returnsBoolean() {
        when(reviewService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = reviewController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(reviewService).removeById(1L);
    }

    @Test
    void delete_returnsFalse_whenServiceFails() {
        when(reviewService.removeById(1L)).thenReturn(false);

        Result<Boolean> result = reviewController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void get_found() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        when(reviewService.getById(1L)).thenReturn(record);

        Result<SfReviewRecord> result = reviewController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(record);
    }

    @Test
    void get_notFound() {
        when(reviewService.getById(1L)).thenReturn(null);

        Result<SfReviewRecord> result = reviewController.get(1L);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void list_returnsRecords() {
        SfReviewRecord record = new SfReviewRecord();
        record.setId(1L);
        when(reviewService.list()).thenReturn(List.of(record));

        Result<List<SfReviewRecord>> result = reviewController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void list_returnsEmptyList() {
        when(reviewService.list()).thenReturn(Collections.emptyList());

        Result<List<SfReviewRecord>> result = reviewController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
