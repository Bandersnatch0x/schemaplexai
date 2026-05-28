package com.schemaplexai.agent.engine.persistence;

import com.schemaplexai.agent.engine.mapper.UuidTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UuidTypeHandlerTest {

    private final UuidTypeHandler typeHandler = new UuidTypeHandler();

    @Test
    void setNonNullParameterBindsUuidAsJdbcOther() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID uuid = UUID.randomUUID();

        typeHandler.setNonNullParameter(statement, 1, uuid, JdbcType.OTHER);

        verify(statement).setObject(1, uuid, Types.OTHER);
    }

    @Test
    void getNullableResultReturnsUuidValue() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject("event_id")).thenReturn(uuid);

        UUID result = typeHandler.getNullableResult(resultSet, "event_id");

        assertThat(result).isEqualTo(uuid);
    }

    @Test
    void getNullableResultParsesStringValue() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject("event_id")).thenReturn(uuid.toString());

        UUID result = typeHandler.getNullableResult(resultSet, "event_id");

        assertThat(result).isEqualTo(uuid);
    }

    @Test
    void getNullableResultReturnsNullForNullValue() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("event_id")).thenReturn(null);

        UUID result = typeHandler.getNullableResult(resultSet, "event_id");

        assertThat(result).isNull();
    }
}
