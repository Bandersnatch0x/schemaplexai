package com.schemaplexai.quality.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.quality.entity.SfProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper
public interface SfProcessedEventMapper extends BaseMapperX<SfProcessedEvent> {

    default boolean exists(UUID eventId, String consumerName) {
        return selectCount(new LambdaQueryWrapper<SfProcessedEvent>()
                .eq(SfProcessedEvent::getEventId, eventId)
                .eq(SfProcessedEvent::getConsumerName, consumerName)) > 0;
    }

    default int insertProcessed(UUID eventId, String consumerName) {
        SfProcessedEvent record = new SfProcessedEvent();
        record.setEventId(eventId);
        record.setConsumerName(consumerName);
        record.setProcessedAt(LocalDateTime.now());
        return insert(record);
    }
}
