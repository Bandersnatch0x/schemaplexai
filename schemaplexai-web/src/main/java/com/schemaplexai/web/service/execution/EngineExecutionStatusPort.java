package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EngineExecutionStatusPort implements ExecutionStatusPort {

    private final ObjectProvider<SfAgentExecutionMapper> executionMapperProvider;

    @Override
    public Map<String, Object> getExecutionStatus(Long executionId) {
        validateExecutionId(executionId);

        SfAgentExecution execution = executionMapper().selectById(executionId);
        if (execution == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Execution not found: " + executionId);
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("executionId", execution.getId());
        status.put("tenantId", execution.getTenantId());
        status.put("agentId", execution.getAgentId());
        status.put("conversationId", execution.getConversationId());
        status.put("status", execution.getState());
        status.put("snapshotId", execution.getSnapshotId());
        status.put("skillName", execution.getSkillName());
        status.put("roleName", execution.getRoleName());
        status.put("version", execution.getVersion());
        status.put("lastEventSeq", execution.getLastEventSeq());
        status.put("createdAt", execution.getCreatedAt());
        status.put("updatedAt", execution.getUpdatedAt());
        status.put("completedAt", execution.getCompletedAt());
        return status;
    }

    private void validateExecutionId(Long executionId) {
        if (executionId == null || executionId <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Execution id must be a positive number");
        }
    }

    private SfAgentExecutionMapper executionMapper() {
        SfAgentExecutionMapper executionMapper = executionMapperProvider.getIfAvailable();
        if (executionMapper == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Execution status service is not available in web runtime");
        }
        return executionMapper;
    }
}
