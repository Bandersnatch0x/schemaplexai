package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.service.SpecReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecReviewControllerTest {

    @Mock
    private SpecReviewService specReviewService;

    @InjectMocks
    private SpecReviewController specReviewController;

    private SfSpecReview review;

    @BeforeEach
    void setUp() {
        review = new SfSpecReview();
        review.setId(1L);
        review.setSpecId(1L);
        review.setReviewerId(2L);
        review.setStatus("approved");
        review.setComment("LGTM");
    }

    // ========== CRUD ==========

    @Test
    void create_returnsId() {
        when(specReviewService.save(any())).thenReturn(true);

        Result<Long> result = specReviewController.create(review);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(specReviewService.updateById(any())).thenReturn(true);

        Result<Boolean> result = specReviewController.update(1L, review);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(specReviewService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = specReviewController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(specReviewService.getById(1L)).thenReturn(review);

        Result<SfSpecReview> result = specReviewController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getComment()).isEqualTo("LGTM");
    }

    @Test
    void get_notFound() {
        when(specReviewService.getById(1L)).thenReturn(null);

        Result<SfSpecReview> result = specReviewController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void page_returnsPageResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecReview> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(review));
        when(specReviewService.page(any())).thenReturn(mpPage);

        PageParam pageParam = new PageParam();
        pageParam.setCurrent(1L);
        pageParam.setSize(10L);

        Result<PageResult<SfSpecReview>> result = specReviewController.page(pageParam);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getTotal()).isEqualTo(1L);
    }

    // ========== Custom endpoints ==========

    @Test
    void submitReview_returnsReview() {
        when(specReviewService.submitReview(1L, 2L, "approved", "LGTM")).thenReturn(review);

        Result<SfSpecReview> result = specReviewController.submitReview(1L, 2L, "approved", "LGTM");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getStatus()).isEqualTo("approved");
    }

    @Test
    void submitReview_withoutComment_returnsReview() {
        when(specReviewService.submitReview(1L, 2L, "rejected", null)).thenReturn(review);

        Result<SfSpecReview> result = specReviewController.submitReview(1L, 2L, "rejected", null);

        assertThat(result.getCode()).isEqualTo(200);
    }
}
