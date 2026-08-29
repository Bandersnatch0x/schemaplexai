package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecReviewMapper;
import com.schemaplexai.spec.service.SpecReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SpecReviewServiceImpl extends ServiceImpl<SfSpecReviewMapper, SfSpecReview> implements SpecReviewService {

    private final SfSpecMapper specMapper;

    @Override
    public SfSpecReview submitReview(Long specId, Long reviewerId, String status, String comment) {
        if (specId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "specId is required");
        }
        if (reviewerId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "reviewerId is required");
        }
        if (status == null || status.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "status is required");
        }
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND, "Spec not found: " + specId);
        }

        SfSpecReview review = new SfSpecReview();
        review.setSpecId(specId);
        review.setReviewerId(reviewerId);
        review.setStatus(status);
        review.setComment(comment);
        baseMapper.insert(review);

        // Map the review decision onto the spec lifecycle vocabulary
        // (draft / in_review / approved / published / archived).
        // The raw decision value is kept on sf_spec_review.status.
        if ("REJECTED".equalsIgnoreCase(status) || "CHANGES_REQUESTED".equalsIgnoreCase(status)) {
            spec.setStatus("draft");
            specMapper.updateById(spec);
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            spec.setStatus("approved");
            specMapper.updateById(spec);
        }

        log.info("Review submitted for spec {} by reviewer {} with status {}", specId, reviewerId, status);
        return review;
    }
}
