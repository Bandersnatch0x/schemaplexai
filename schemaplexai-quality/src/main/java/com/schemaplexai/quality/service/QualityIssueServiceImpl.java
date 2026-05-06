package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class QualityIssueServiceImpl extends ServiceImpl<QualityIssueMapper, SfQualityIssue> implements QualityIssueService {

    private static final int STATUS_OPEN = 0;
    private static final int STATUS_IN_PROGRESS = 1;
    private static final int STATUS_RESOLVED = 2;
    private static final int STATUS_CLOSED = 3;
    private static final int STATUS_WONT_FIX = 4;

    private static final String SEVERITY_CRITICAL = "CRITICAL";
    private static final String SEVERITY_HIGH = "HIGH";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_LOW = "LOW";

    /**
     * Create a new quality issue with validation.
     */
    @Override
    public boolean save(SfQualityIssue issue) {
        validateIssue(issue);
        if (issue.getStatus() == null) {
            issue.setStatus(STATUS_OPEN);
        }
        log.info("Creating quality issue: executionId={}, type={}, severity={}",
            issue.getExecutionId(), issue.getIssueType(), issue.getSeverity());
        return super.save(issue);
    }

    /**
     * List all open issues for a given execution.
     */
    public List<SfQualityIssue> listOpenIssuesByExecution(Long executionId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfQualityIssue>()
                .eq(SfQualityIssue::getExecutionId, executionId)
                .eq(SfQualityIssue::getStatus, STATUS_OPEN)
                .orderByDesc(SfQualityIssue::getCreatedAt));
    }

    /**
     * List all issues for a given execution, ordered by severity.
     */
    public List<SfQualityIssue> listIssuesByExecution(Long executionId) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfQualityIssue>()
                .eq(SfQualityIssue::getExecutionId, executionId)
                .orderByAsc(SfQualityIssue::getSeverity)
                .orderByDesc(SfQualityIssue::getCreatedAt));
    }

    /**
     * Resolve an issue with optional resolution notes stored in description.
     */
    public void resolveIssue(Long issueId, String resolutionNotes) {
        SfQualityIssue issue = baseMapper.selectById(issueId);
        if (issue == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality issue not found: " + issueId);
        }
        if (issue.getStatus() == STATUS_RESOLVED || issue.getStatus() == STATUS_CLOSED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Issue is already resolved or closed");
        }
        issue.setStatus(STATUS_RESOLVED);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            String updatedDescription = issue.getDescription() + "\n[Resolution: " + resolutionNotes + "]";
            issue.setDescription(updatedDescription);
        }
        baseMapper.updateById(issue);
        log.info("Resolved quality issue: id={}", issueId);
    }

    /**
     * Mark an issue as won't fix with a reason.
     */
    public void markAsWontFix(Long issueId, String reason) {
        SfQualityIssue issue = baseMapper.selectById(issueId);
        if (issue == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality issue not found: " + issueId);
        }
        if (issue.getStatus() == STATUS_RESOLVED || issue.getStatus() == STATUS_CLOSED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot mark resolved/closed issue as won't fix");
        }
        issue.setStatus(STATUS_WONT_FIX);
        if (reason != null && !reason.isBlank()) {
            String updatedDescription = issue.getDescription() + "\n[Won't Fix: " + reason + "]";
            issue.setDescription(updatedDescription);
        }
        baseMapper.updateById(issue);
        log.info("Marked quality issue as won't fix: id={}", issueId);
    }

    /**
     * Reopen a resolved or closed issue.
     */
    public void reopenIssue(Long issueId) {
        SfQualityIssue issue = baseMapper.selectById(issueId);
        if (issue == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality issue not found: " + issueId);
        }
        if (issue.getStatus() == STATUS_OPEN || issue.getStatus() == STATUS_IN_PROGRESS) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Issue is already open or in progress");
        }
        issue.setStatus(STATUS_OPEN);
        baseMapper.updateById(issue);
        log.info("Reopened quality issue: id={}", issueId);
    }

    /**
     * Get severity distribution for an execution.
     */
    public SeverityDistribution getSeverityDistribution(Long executionId) {
        List<SfQualityIssue> issues = baseMapper.selectList(
            new LambdaQueryWrapper<SfQualityIssue>()
                .eq(SfQualityIssue::getExecutionId, executionId));

        Map<String, Long> distribution = issues.stream()
            .collect(Collectors.groupingBy(
                i -> i.getSeverity() != null ? i.getSeverity() : "UNKNOWN",
                Collectors.counting()));

        return new SeverityDistribution(
            executionId,
            distribution.getOrDefault(SEVERITY_CRITICAL, 0L),
            distribution.getOrDefault(SEVERITY_HIGH, 0L),
            distribution.getOrDefault(SEVERITY_MEDIUM, 0L),
            distribution.getOrDefault(SEVERITY_LOW, 0L),
            issues.size()
        );
    }

    /**
     * Bulk update status for issues matching a severity in an execution.
     */
    public int bulkUpdateStatusBySeverity(Long executionId, String severity, Integer newStatus) {
        List<SfQualityIssue> issues = baseMapper.selectList(
            new LambdaQueryWrapper<SfQualityIssue>()
                .eq(SfQualityIssue::getExecutionId, executionId)
                .eq(SfQualityIssue::getSeverity, severity));

        int updated = 0;
        for (SfQualityIssue issue : issues) {
            issue.setStatus(newStatus);
            baseMapper.updateById(issue);
            updated++;
        }
        log.info("Bulk updated {} issues to status {} for execution {} with severity {}",
            updated, newStatus, executionId, severity);
        return updated;
    }

    private void validateIssue(SfQualityIssue issue) {
        if (issue.getExecutionId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Execution ID is required");
        }
        if (issue.getIssueType() == null || issue.getIssueType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Issue type is required");
        }
        if (issue.getSeverity() != null) {
            String sev = issue.getSeverity().toUpperCase();
            if (!sev.equals(SEVERITY_CRITICAL) && !sev.equals(SEVERITY_HIGH)
                && !sev.equals(SEVERITY_MEDIUM) && !sev.equals(SEVERITY_LOW)) {
                throw new BaseException(ResultCode.PARAM_ERROR,
                    "Severity must be one of: CRITICAL, HIGH, MEDIUM, LOW");
            }
            issue.setSeverity(sev);
        }
    }

    public record SeverityDistribution(Long executionId, long criticalCount, long highCount,
                                        long mediumCount, long lowCount, long totalCount) {
    }
}
