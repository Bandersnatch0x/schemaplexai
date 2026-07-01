package com.schemaplexai.web.mapper;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import org.springframework.stereotype.Component;

/**
 * M6.4: Execution entity to VO mapper.
 */
@Component
public class ExecutionMapper {

    public ExecutionStatusVO toStatusVO(SfAgentExecution entity) {
        if (entity == null) {
            return null;
        }
        ExecutionStatusVO vo = new ExecutionStatusVO();
        vo.setExecutionId(entity.getId());
        vo.setAgentId(entity.getAgentId());
        vo.setState(entity.getState());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
