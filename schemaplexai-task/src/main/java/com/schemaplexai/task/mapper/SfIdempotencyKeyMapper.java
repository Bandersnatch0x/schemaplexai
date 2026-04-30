package com.schemaplexai.task.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.task.entity.SfIdempotencyKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SfIdempotencyKeyMapper extends BaseMapperX<SfIdempotencyKey> {
}
