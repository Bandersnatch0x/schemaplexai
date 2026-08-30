package com.schemaplexai.web.service.execution;

import com.schemaplexai.web.vo.ExecutionStatusVO;

public interface ExecutionStatusPort {

    ExecutionStatusVO getExecutionStatus(Long executionId);
}
