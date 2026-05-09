package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Optimistic-locking concurrency guard for execution state updates.
 */
@Service
@RequiredArgsConstructor
public class ExecutionConcurrencyService {

    private final SfAgentExecutionMapper executionMapper;

    public void updateState(Long executionId, String newState, int expectedVersion) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "execution not found: " + executionId);
        }
        if (!Integer.valueOf(expectedVersion).equals(execution.getVersion())) {
            throw new BaseException(ResultCode.CONFLICT,
                    "version conflict: expected " + expectedVersion + " but current is " + execution.getVersion());
        }
        execution.setState(newState);
        executionMapper.updateById(execution);
    }
}
