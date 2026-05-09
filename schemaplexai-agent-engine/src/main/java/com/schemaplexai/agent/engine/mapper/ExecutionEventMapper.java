package com.schemaplexai.agent.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExecutionEventMapper extends BaseMapperX<ExecutionEvent> {

    default List<ExecutionEvent> selectByExecutionIdOrdered(Long executionId) {
        return selectList(new LambdaQueryWrapper<ExecutionEvent>()
                .eq(ExecutionEvent::getExecutionId, executionId)
                .orderByAsc(ExecutionEvent::getSeq));
    }

    @Select("SELECT DISTINCT execution_id FROM sf_execution_event " +
            "WHERE occurred_at > NOW() - INTERVAL '1 hour' ORDER BY execution_id")
    List<Long> selectActiveExecutionIds();
}
