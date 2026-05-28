package com.schemaplexai.web.service.execution;

import java.util.Map;

public interface ExecutionStatusPort {

    Map<String, Object> getExecutionStatus(Long executionId);
}
