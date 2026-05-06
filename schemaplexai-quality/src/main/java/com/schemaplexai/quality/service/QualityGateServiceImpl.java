package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class QualityGateServiceImpl extends ServiceImpl<QualityGateMapper, SfQualityGate> implements QualityGateService {

    private final QualityIssueMapper qualityIssueMapper;
    private final QualityOrchestrator qualityOrchestrator;

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 0;
    private static final int STATUS_DEPRECATED = 2;

    /**
     * Create a new quality gate with validation.
     */
    @Override
    public boolean save(SfQualityGate gate) {
        validateGate(gate);
        if (gate.getStatus() == null) {
            gate.setStatus(STATUS_INACTIVE);
        }
        log.info("Creating quality gate: name={}", gate.getName());
        return super.save(gate);
    }

    /**
     * Update a quality gate with validation.
     */
    @Override
    public boolean updateById(SfQualityGate gate) {
        if (gate.getId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gate ID is required for update");
        }
        SfQualityGate existing = baseMapper.selectById(gate.getId());
        if (existing == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality gate not found: " + gate.getId());
        }
        if (gate.getName() != null) {
            validateGate(gate);
        }
        log.info("Updating quality gate: id={}, name={}", gate.getId(), gate.getName());
        return super.updateById(gate);
    }

    /**
     * Activate a quality gate, making it eligible for evaluation.
     */
    public void activateGate(Long gateId) {
        SfQualityGate gate = baseMapper.selectById(gateId);
        if (gate == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality gate not found: " + gateId);
        }
        if (gate.getStatus() != null && gate.getStatus() == STATUS_DEPRECATED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot activate a deprecated gate");
        }
        gate.setStatus(STATUS_ACTIVE);
        baseMapper.updateById(gate);
        log.info("Activated quality gate: id={}", gateId);
    }

    /**
     * Deactivate a quality gate, preventing it from being evaluated.
     */
    public void deactivateGate(Long gateId) {
        SfQualityGate gate = baseMapper.selectById(gateId);
        if (gate == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality gate not found: " + gateId);
        }
        gate.setStatus(STATUS_INACTIVE);
        baseMapper.updateById(gate);
        log.info("Deactivated quality gate: id={}", gateId);
    }

    /**
     * Deprecate a quality gate. Deprecated gates cannot be reactivated.
     */
    public void deprecateGate(Long gateId) {
        SfQualityGate gate = baseMapper.selectById(gateId);
        if (gate == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Quality gate not found: " + gateId);
        }
        gate.setStatus(STATUS_DEPRECATED);
        baseMapper.updateById(gate);
        log.info("Deprecated quality gate: id={}", gateId);
    }

    /**
     * Evaluate a single quality gate by name for a given execution.
     */
    public QualityReport evaluateGate(Long executionId, String gateName) {
        SfQualityGate gate = baseMapper.selectOne(
            new LambdaQueryWrapper<SfQualityGate>()
                .eq(SfQualityGate::getName, gateName)
                .eq(SfQualityGate::getStatus, STATUS_ACTIVE));
        if (gate == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Active quality gate not found: " + gateName);
        }
        QualityContext context = new QualityContext(executionId, null, java.util.Map.of());
        return qualityOrchestrator.evaluate(executionId, context);
    }

    /**
     * List all active quality gates.
     */
    public List<SfQualityGate> listActiveGates() {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfQualityGate>()
                .eq(SfQualityGate::getStatus, STATUS_ACTIVE));
    }

    /**
     * Get the quality gate summary for an execution, including pass/fail counts.
     */
    public GateSummary getGateSummary(Long executionId) {
        List<SfQualityIssue> issues = qualityIssueMapper.selectList(
            new LambdaQueryWrapper<SfQualityIssue>()
                .eq(SfQualityIssue::getExecutionId, executionId));

        long criticalCount = issues.stream().filter(i -> "CRITICAL".equals(i.getSeverity())).count();
        long highCount = issues.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
        long mediumCount = issues.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
        long lowCount = issues.stream().filter(i -> "LOW".equals(i.getSeverity())).count();
        long openCount = issues.stream().filter(i -> i.getStatus() != null && i.getStatus() == 0).count();

        return new GateSummary(executionId, issues.size(), openCount, criticalCount, highCount, mediumCount, lowCount);
    }

    private void validateGate(SfQualityGate gate) {
        if (gate.getName() == null || gate.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gate name is required");
        }
        if (gate.getName().length() > 128) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Gate name must not exceed 128 characters");
        }
    }

    public record GateSummary(Long executionId, long totalIssues, long openIssues,
                               long criticalCount, long highCount, long mediumCount, long lowCount) {
    }
}
