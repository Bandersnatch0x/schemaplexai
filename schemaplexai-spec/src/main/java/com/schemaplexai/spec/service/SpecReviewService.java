package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.entity.SfSpecReview;

public interface SpecReviewService extends IService<SfSpecReview> {

    SfSpecReview submitReview(Long specId, Long reviewerId, String status, String comment);
}
