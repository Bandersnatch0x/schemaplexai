package com.schemaplexai.web.service.execution;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.web.mapper.ExecutionMapper;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * M6.4: Execution query port — paginated execution list with filtering.
 */
@Service
@RequiredArgsConstructor
public class EngineExecutionQueryPort {

    private final ObjectProvider<SfAgentExecutionMapper> executionMapperProvider;
    private final ExecutionMapper executionMapper;

    public IPage<ExecutionStatusVO> listExecutions(long page, long size, String state, Long agentId) {
        SfAgentExecutionMapper mapper = executionMapper();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAgentExecution> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (state != null && !state.isBlank()) {
            wrapper.eq(SfAgentExecution::getState, state);
        }
        if (agentId != null) {
            wrapper.eq(SfAgentExecution::getAgentId, agentId);
        }
        wrapper.orderByDesc(SfAgentExecution::getId);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfAgentExecution> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        IPage<SfAgentExecution> entityPage = mapper.selectPage(mpPage, wrapper);
        return entityPage.convert(executionMapper::toStatusVO);
    }

    private SfAgentExecutionMapper executionMapper() {
        SfAgentExecutionMapper mapper = executionMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Execution query service is not available in web runtime");
        }
        return mapper;
    }
}
