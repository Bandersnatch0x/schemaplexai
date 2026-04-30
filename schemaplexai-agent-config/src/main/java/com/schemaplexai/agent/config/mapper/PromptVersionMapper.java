package com.schemaplexai.agent.config.mapper;

import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromptVersionMapper extends BaseMapperX<SfPromptVersion> {
}
