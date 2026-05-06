package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.mapper.ReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewRecordMapper, SfReviewRecord> implements ReviewService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ASSIGNED = 1;
    private static final int STATUS_IN_REVIEW = 2;
    private static final int STATUS_APPROVED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int STATUS_CHANGES_REQUESTED = 5;

    /**
     * Create a new review record with validation.
     */
    @Override
    public boolean save(SfReviewRecord record) {
        validateReviewRecord(record);
        if (record.getStatus() == null) {
            record.setStatus(STATUS_PENDING);
        }
        log.info("Creating review record: resourceType={}, resourceId={}",
            record.getResourceType(), record.getResourceId());
        return super.save(record);
    }

    /**
     * Assign a review to a reviewer.
     */
    public void assignReview(Long reviewId, Long reviewerId) {
        if (reviewerId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Reviewer ID is required");
        }
        SfReviewRecord record = baseMapper.selectById(reviewId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Review record not found: " + reviewId);
        }
        if (record.getStatus() != STATUS_PENDING && record.getStatus() != STATUS_ASSIGNED) {
            throw new BaseException(ResultCode.PARAM_ERROR,
                "Cannot assign review that is already in progress or completed");
        }
        record.setReviewerId(reviewerId);
        record.setStatus(STATUS_ASSIGNED);
        baseMapper.updateById(record);
        log.info("Assigned review {} to reviewer {}", reviewId, reviewerId);
    }

    /**
     * Start reviewing a record.
     */
    public void startReview(Long reviewId) {
        SfReviewRecord record = baseMapper.selectById(reviewId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Review record not found: " + reviewId);
        }
        if (record.getReviewerId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review must be assigned to a reviewer first");
        }
        if (record.getStatus() == STATUS_IN_REVIEW) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review is already in progress");
        }
        if (record.getStatus() == STATUS_APPROVED || record.getStatus() == STATUS_REJECTED
            || record.getStatus() == STATUS_CHANGES_REQUESTED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review is already completed");
        }
        record.setStatus(STATUS_IN_REVIEW);
        baseMapper.updateById(record);
        log.info("Started review: id={}", reviewId);
    }

    /**
     * Submit an approval for a review.
     */
    public void approveReview(Long reviewId, String comment) {
        SfReviewRecord record = baseMapper.selectById(reviewId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Review record not found: " + reviewId);
        }
        if (record.getStatus() == STATUS_APPROVED || record.getStatus() == STATUS_REJECTED
            || record.getStatus() == STATUS_CHANGES_REQUESTED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review is already completed");
        }
        record.setStatus(STATUS_APPROVED);
        if (comment != null && !comment.isBlank()) {
            record.setComment(appendComment(record.getComment(), "APPROVED: " + comment));
        }
        baseMapper.updateById(record);
        log.info("Approved review: id={}", reviewId);
    }

    /**
     * Submit a rejection for a review.
     */
    public void rejectReview(Long reviewId, String reason) {
        SfReviewRecord record = baseMapper.selectById(reviewId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Review record not found: " + reviewId);
        }
        if (record.getStatus() == STATUS_APPROVED || record.getStatus() == STATUS_REJECTED
            || record.getStatus() == STATUS_CHANGES_REQUESTED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review is already completed");
        }
        record.setStatus(STATUS_REJECTED);
        if (reason != null && !reason.isBlank()) {
            record.setComment(appendComment(record.getComment(), "REJECTED: " + reason));
        }
        baseMapper.updateById(record);
        log.info("Rejected review: id={}", reviewId);
    }

    /**
     * Request changes for a review.
     */
    public void requestChanges(Long reviewId, String feedback) {
        SfReviewRecord record = baseMapper.selectById(reviewId);
        if (record == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Review record not found: " + reviewId);
        }
        if (record.getStatus() == STATUS_APPROVED || record.getStatus() == STATUS_REJECTED
            || record.getStatus() == STATUS_CHANGES_REQUESTED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Review is already completed");
        }
        record.setStatus(STATUS_CHANGES_REQUESTED);
        if (feedback != null && !feedback.isBlank()) {
            record.setComment(appendComment(record.getComment(), "CHANGES_REQUESTED: " + feedback));
        }
        baseMapper.updateById(record);
        log.info("Requested changes for review: id={}", reviewId);
    }

    /**
     * List pending reviews for a reviewer.
     */
    public List<SfReviewRecord> listPendingReviewsForReviewer(Long reviewerId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfReviewRecord>()
                .eq(SfReviewRecord::getReviewerId, reviewerId)
                .in(SfReviewRecord::getStatus, STATUS_ASSIGNED, STATUS_IN_REVIEW)
                .orderByDesc(SfReviewRecord::getCreatedAt));
    }

    /**
     * List all reviews for a resource.
     */
    public List<SfReviewRecord> listReviewsForResource(String resourceType, Long resourceId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfReviewRecord>()
                .eq(SfReviewRecord::getResourceType, resourceType)
                .eq(SfReviewRecord::getResourceId, resourceId)
                .orderByDesc(SfReviewRecord::getCreatedAt));
    }

    /**
     * Get the latest review for a resource.
     */
    public SfReviewRecord getLatestReviewForResource(String resourceType, Long resourceId) {
        List<SfReviewRecord> reviews = baseMapper.selectList(
            new LambdaQueryWrapper<SfReviewRecord>()
                .eq(SfReviewRecord::getResourceType, resourceType)
                .eq(SfReviewRecord::getResourceId, resourceId)
                .orderByDesc(SfReviewRecord::getCreatedAt)
                .last("LIMIT 1"));
        return reviews.isEmpty() ? null : reviews.get(0);
    }

    /**
     * Check if a resource has an approved review.
     */
    public boolean hasApprovedReview(String resourceType, Long resourceId) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<SfReviewRecord>()
                .eq(SfReviewRecord::getResourceType, resourceType)
                .eq(SfReviewRecord::getResourceId, resourceId)
                .eq(SfReviewRecord::getStatus, STATUS_APPROVED));
        return count != null && count > 0;
    }

    private void validateReviewRecord(SfReviewRecord record) {
        if (record.getResourceType() == null || record.getResourceType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Resource type is required");
        }
        if (record.getResourceId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Resource ID is required");
        }
    }

    private String appendComment(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return existing + "\n" + addition;
    }
}
