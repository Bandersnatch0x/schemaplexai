package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.lifecycle.PauseReason;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EngineExecutionLifecyclePort implements ExecutionLifecyclePort {

    private final ObjectProvider<AgentExecutionLifecycleService> lifecycleServiceProvider;

    @Override
    public void pauseExecution(Long executionId) {
        lifecycleService().pauseExecution(executionId, PauseReason.USER_REQUEST);
    }

    @Override
    public void resumeExecution(Long executionId) {
        lifecycleService().resumeExecution(executionId);
    }

    @Override
    public void cancelExecution(Long executionId) {
        lifecycleService().cancelExecution(executionId);
    }

    private AgentExecutionLifecycleService lifecycleService() {
        AgentExecutionLifecycleService lifecycleService = lifecycleServiceProvider.getIfAvailable();
        if (lifecycleService == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Execution lifecycle service is not available in web runtime");
        }
        return lifecycleService;
    }
}
