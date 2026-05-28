package com.schemaplexai.agent.engine.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ProcessedEventMapper;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityMappingTest {

    @Test
    void executionEventUsesEventIdAsMyBatisPlusPrimaryKey() {
        TableInfoHelper.remove(ExecutionEvent.class);
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "event-entity-mapping-test"),
                ExecutionEvent.class);

        assertThat(tableInfo).isNotNull();
        assertThat(tableInfo.getKeyProperty()).isEqualTo("eventId");
        assertThat(tableInfo.getKeyColumn()).isEqualTo("event_id");
    }

    @Test
    void processedEventMapperDoesNotExposeSingleColumnCrudForCompositePrimaryKey() {
        assertThat(BaseMapperX.class.isAssignableFrom(ProcessedEventMapper.class)).isFalse();
    }
}
