package com.schemaplexai.agent.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.ExecutionSnapshot;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExecutionSnapshotMapper extends BaseMapperX<ExecutionSnapshot> {

    default ExecutionSnapshot selectLatestByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapper<ExecutionSnapshot>()
                .eq(ExecutionSnapshot::getExecutionId, executionId)
                .orderByDesc(ExecutionSnapshot::getCreatedAt)
                .last("LIMIT 1"));
    }
}
