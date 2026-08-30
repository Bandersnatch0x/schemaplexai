package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.domain.SpecStatus;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecReview;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecReviewMapper;
import com.schemaplexai.spec.service.SpecChangeTracker;
import com.schemaplexai.spec.service.SpecReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SpecReviewServiceImpl extends ServiceImpl<SfSpecReviewMapper, SfSpecReview> implements SpecReviewService {

    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_CHANGES_REQUESTED = "CHANGES_REQUESTED";
    private static final String DECISION_REJECTED = "REJECTED";

    private final SfSpecMapper specMapper;
    private final SpecChangeTracker changeTracker;

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
        String decision = status.trim().toUpperCase(Locale.ROOT);
        if (!DECISION_APPROVED.equals(decision)
                && !DECISION_CHANGES_REQUESTED.equals(decision)
                && !DECISION_REJECTED.equals(decision)) {
            // §4.1 defines exactly three decisions; anything else is a client
            // error, not a silently-passing fourth branch.
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "Unsupported review decision: " + status
                            + " (expected APPROVED / CHANGES_REQUESTED / REJECTED)");
        }

        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND, "Spec not found: " + specId);
        }
        if (!SpecStatus.isReviewable(spec.getStatus())) {
            throw new BaseException(ResultCode.FORBIDDEN,
                    "Spec " + specId + " cannot be reviewed in status " + spec.getStatus());
        }

        SfSpecReview review = new SfSpecReview();
        review.setSpecId(specId);
        review.setReviewerId(reviewerId);
        // Store the normalized decision (§4.2 vocabulary), not the raw casing.
        review.setStatus(decision);
        review.setComment(comment);
        baseMapper.insert(review);

        // Map the review decision onto the spec lifecycle. The three branches
        // are independent (spec-management §4.1):
        //   APPROVED          -> approved   (通过发布: eligible to publish)
        //   CHANGES_REQUESTED -> draft      (返回修改: back to editing)
        //   REJECTED          -> rejected   (结束流程: terminal, not editable,
        //                                    not re-reviewable)
        SfSpec before = SpecChangeTracker.snapshot(spec);
        switch (decision) {
            case DECISION_APPROVED -> spec.setStatus(SpecStatus.APPROVED);
            case DECISION_CHANGES_REQUESTED -> spec.setStatus(SpecStatus.DRAFT);
            case DECISION_REJECTED -> spec.setStatus(SpecStatus.REJECTED);
            default -> throw new IllegalStateException("unreachable decision " + decision);
        }
        int rows = specMapper.updateById(spec);
        if (rows == 0) {
            throw new BaseException(ResultCode.CONFLICT,
                    "Spec " + specId + " was modified concurrently; reload and retry");
        }
        changeTracker.recordUpdate(before, spec, null);

        log.info("Review submitted for spec {} by reviewer {} with decision {}", specId, reviewerId, decision);
        return review;
    }
}
