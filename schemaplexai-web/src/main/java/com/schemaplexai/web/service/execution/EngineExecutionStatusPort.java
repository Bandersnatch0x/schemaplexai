package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.web.mapper.ExecutionMapper;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EngineExecutionStatusPort implements ExecutionStatusPort {

    private final ObjectProvider<SfAgentExecutionMapper> executionMapperProvider;
    private final ExecutionMapper executionMapper;

    @Override
    public ExecutionStatusVO getExecutionStatus(Long executionId) {
        validateExecutionId(executionId);

        SfAgentExecution execution = executionMapper().selectById(executionId);
        if (execution == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Execution not found: " + executionId);
        }

        return executionMapper.toStatusVO(execution);
    }

    private void validateExecutionId(Long executionId) {
        if (executionId == null || executionId <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Execution id must be a positive number");
        }
    }

    private SfAgentExecutionMapper executionMapper() {
        SfAgentExecutionMapper mapper = executionMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Execution status service is not available in web runtime");
        }
        return mapper;
    }
}
