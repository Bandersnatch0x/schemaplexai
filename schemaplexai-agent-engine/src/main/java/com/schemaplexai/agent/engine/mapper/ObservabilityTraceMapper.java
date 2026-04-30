package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObservabilityTraceMapper extends BaseMapperX<ObservabilityTrace> {
}
