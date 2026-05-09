package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.model.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapperX<ProcessedEvent> {
}