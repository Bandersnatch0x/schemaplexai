package com.schemaplexai.agent.engine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExecutionOutboxMapper extends BaseMapperX<ExecutionOutbox> {

    /**
     * Selects unpublished entries with retry_count below the threshold.
     */
    default List<ExecutionOutbox> selectUnpublished(int maxRetries) {
        return selectList(new LambdaQueryWrapper<ExecutionOutbox>()
                .isNull(ExecutionOutbox::getPublishedAt)
                .lt(ExecutionOutbox::getRetryCount, maxRetries)
                .orderByAsc(ExecutionOutbox::getCreatedAt)
                .last("LIMIT 100"));
    }
}
