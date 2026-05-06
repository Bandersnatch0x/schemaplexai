package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.mapper.EvalTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl extends ServiceImpl<EvalTaskMapper, SfEvalTask> implements EvaluationService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_FAILED = 3;

    @Override
    public SfEvalTask runEvaluation(Long evalTaskId) {
        SfEvalTask task = baseMapper.selectById(evalTaskId);
        if (task == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Evaluation task not found: " + evalTaskId);
        }
        if (task.getStatus() != null && task.getStatus() == STATUS_RUNNING) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Evaluation is already running: " + evalTaskId);
        }
        task.setStatus(STATUS_RUNNING);
        task.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(task);
        log.info("Started evaluation task: id={}, datasetId={}, agentId={}", evalTaskId, task.getDatasetId(), task.getAgentId());
        return task;
    }

    @Override
    public SfEvalTask getEvaluationResults(Long evalTaskId) {
        SfEvalTask task = baseMapper.selectById(evalTaskId);
        if (task == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Evaluation task not found: " + evalTaskId);
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(task.getTenantId())) {
            throw new BaseException(ResultCode.FORBIDDEN, "Access denied to evaluation task: " + evalTaskId);
        }
        return task;
    }

    @Override
    public List<SfEvalTask> listByDataset(Long datasetId) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfEvalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfEvalTask::getDatasetId, datasetId);
        if (tenantId != null) {
            wrapper.eq(SfEvalTask::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfEvalTask::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<SfEvalTask> listByStatus(Integer status) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfEvalTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SfEvalTask::getStatus, status);
        }
        if (tenantId != null) {
            wrapper.eq(SfEvalTask::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfEvalTask::getCreatedAt);
        return baseMapper.selectList(wrapper);
    }
}
