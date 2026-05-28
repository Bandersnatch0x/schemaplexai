package com.schemaplexai.agent.engine.mapper;

import com.schemaplexai.model.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface ProcessedEventMapper {

    @Select("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM sf_processed_event
            WHERE event_id = #{eventId,typeHandler=com.schemaplexai.agent.engine.mapper.UuidTypeHandler}
              AND consumer_name = #{consumerName}
            """)
    boolean exists(@Param("eventId") UUID eventId, @Param("consumerName") String consumerName);

    @Insert("""
            INSERT INTO sf_processed_event (event_id, consumer_name, processed_at)
            VALUES (#{eventId,typeHandler=com.schemaplexai.agent.engine.mapper.UuidTypeHandler}, #{consumerName}, #{processedAt})
            """)
    int insertProcessed(ProcessedEvent event);
}
