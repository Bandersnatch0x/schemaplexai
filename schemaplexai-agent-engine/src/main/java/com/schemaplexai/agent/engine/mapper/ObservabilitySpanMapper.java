package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObservabilitySpanMapper extends BaseMapperX<ObservabilitySpan> {
}
