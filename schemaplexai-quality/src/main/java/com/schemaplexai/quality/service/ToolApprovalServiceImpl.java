package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfToolApprovalAmendment;
import com.schemaplexai.quality.mapper.ToolApprovalAmendmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ToolApprovalServiceImpl extends ServiceImpl<ToolApprovalAmendmentMapper, SfToolApprovalAmendment> implements ToolApprovalService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_REVOKED = 3;

    /**
     * Create a new tool approval request with validation.
     */
    @Override
    public boolean save(SfToolApprovalAmendment approval) {
        validateApproval(approval);
        if (approval.getStatus() == null) {
            approval.setStatus(STATUS_PENDING);
        }
        if (approval.getCurrentCount() == null) {
            approval.setCurrentCount(0);
        }
        if (approval.getApprovalThreshold() == null) {
            approval.setApprovalThreshold(1);
        }
        log.info("Creating tool approval: toolName={}, threshold={}",
            approval.getToolName(), approval.getApprovalThreshold());
        return super.save(approval);
    }

    /**
     * Submit an approval vote for a tool. If the threshold is reached, the tool is approved.
     */
    public void submitApprovalVote(Long approvalId, Long voterId) {
        if (voterId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Voter ID is required");
        }
        SfToolApprovalAmendment approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Tool approval not found: " + approvalId);
        }
        if (approval.getStatus() == STATUS_APPROVED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tool is already approved");
        }
        if (approval.getStatus() == STATUS_REJECTED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot vote on a rejected approval");
        }
        if (approval.getStatus() == STATUS_REVOKED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot vote on a revoked approval");
        }

        int newCount = (approval.getCurrentCount() != null ? approval.getCurrentCount() : 0) + 1;
        approval.setCurrentCount(newCount);

        if (newCount >= approval.getApprovalThreshold()) {
            approval.setStatus(STATUS_APPROVED);
            log.info("Tool approval {} reached threshold ({} / {}). Tool '{}' approved.",
                approvalId, newCount, approval.getApprovalThreshold(), approval.getToolName());
        } else {
            log.info("Approval vote submitted for tool '{}': {} / {} votes",
                approval.getToolName(), newCount, approval.getApprovalThreshold());
        }
        baseMapper.updateById(approval);
    }

    /**
     * Reject a tool approval request.
     */
    public void rejectApproval(Long approvalId, String reason) {
        SfToolApprovalAmendment approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Tool approval not found: " + approvalId);
        }
        if (approval.getStatus() == STATUS_APPROVED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot reject an already approved tool");
        }
        if (approval.getStatus() == STATUS_REVOKED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot reject a revoked approval");
        }
        approval.setStatus(STATUS_REJECTED);
        baseMapper.updateById(approval);
        log.info("Rejected tool approval: id={}, tool={}, reason={}", approvalId, approval.getToolName(), reason);
    }

    /**
     * Revoke an approved tool.
     */
    public void revokeApproval(Long approvalId, String reason) {
        SfToolApprovalAmendment approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Tool approval not found: " + approvalId);
        }
        if (approval.getStatus() != STATUS_APPROVED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Only approved tools can be revoked");
        }
        approval.setStatus(STATUS_REVOKED);
        approval.setCurrentCount(0);
        baseMapper.updateById(approval);
        log.info("Revoked tool approval: id={}, tool={}, reason={}", approvalId, approval.getToolName(), reason);
    }

    /**
     * Reset a rejected or revoked approval back to pending.
     */
    public void resetApproval(Long approvalId) {
        SfToolApprovalAmendment approval = baseMapper.selectById(approvalId);
        if (approval == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Tool approval not found: " + approvalId);
        }
        if (approval.getStatus() == STATUS_PENDING) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Approval is already pending");
        }
        approval.setStatus(STATUS_PENDING);
        approval.setCurrentCount(0);
        baseMapper.updateById(approval);
        log.info("Reset tool approval to pending: id={}, tool={}", approvalId, approval.getToolName());
    }

    /**
     * Find approval by tool name.
     */
    public SfToolApprovalAmendment findByToolName(String toolName) {
        List<SfToolApprovalAmendment> approvals = baseMapper.selectList(
            new LambdaQueryWrapper<SfToolApprovalAmendment>()
                .eq(SfToolApprovalAmendment::getToolName, toolName)
                .orderByDesc(SfToolApprovalAmendment::getCreatedAt)
                .last("LIMIT 1"));
        return approvals.isEmpty() ? null : approvals.get(0);
    }

    /**
     * List all pending approvals.
     */
    public List<SfToolApprovalAmendment> listPendingApprovals() {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfToolApprovalAmendment>()
                .eq(SfToolApprovalAmendment::getStatus, STATUS_PENDING)
                .orderByDesc(SfToolApprovalAmendment::getCreatedAt));
    }

    /**
     * List all approved tools.
     */
    public List<SfToolApprovalAmendment> listApprovedTools() {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfToolApprovalAmendment>()
                .eq(SfToolApprovalAmendment::getStatus, STATUS_APPROVED)
                .orderByDesc(SfToolApprovalAmendment::getCreatedAt));
    }

    /**
     * Check if a tool is approved.
     */
    public boolean isToolApproved(String toolName) {
        Long count = baseMapper.selectCount(
            new LambdaQueryWrapper<SfToolApprovalAmendment>()
                .eq(SfToolApprovalAmendment::getToolName, toolName)
                .eq(SfToolApprovalAmendment::getStatus, STATUS_APPROVED));
        return count != null && count > 0;
    }

    /**
     * Get approval status summary.
     */
    public ApprovalSummary getApprovalSummary() {
        long pending = baseMapper.selectCount(
            new LambdaQueryWrapper<SfToolApprovalAmendment>().eq(SfToolApprovalAmendment::getStatus, STATUS_PENDING));
        long approved = baseMapper.selectCount(
            new LambdaQueryWrapper<SfToolApprovalAmendment>().eq(SfToolApprovalAmendment::getStatus, STATUS_APPROVED));
        long rejected = baseMapper.selectCount(
            new LambdaQueryWrapper<SfToolApprovalAmendment>().eq(SfToolApprovalAmendment::getStatus, STATUS_REJECTED));
        long revoked = baseMapper.selectCount(
            new LambdaQueryWrapper<SfToolApprovalAmendment>().eq(SfToolApprovalAmendment::getStatus, STATUS_REVOKED));
        return new ApprovalSummary(pending, approved, rejected, revoked);
    }

    private void validateApproval(SfToolApprovalAmendment approval) {
        if (approval.getToolName() == null || approval.getToolName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tool name is required");
        }
        if (approval.getApprovalThreshold() != null && approval.getApprovalThreshold() < 1) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Approval threshold must be at least 1");
        }
    }

    public record ApprovalSummary(long pendingCount, long approvedCount, long rejectedCount, long revokedCount) {
    }
}
