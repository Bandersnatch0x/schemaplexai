package com.schemaplexai.web.service.execution;

public interface ExecutionLifecyclePort {

    void pauseExecution(Long executionId);

    void resumeExecution(Long executionId);

    void cancelExecution(Long executionId);
}
