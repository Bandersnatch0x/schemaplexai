package com.schemaplexai.agent.config.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SfAgentMapperTest {

    @Test
    void shouldReturnNullWhenTenantIdIsNull() {
        SfAgentMapper mapper = mock(SfAgentMapper.class, CALLS_REAL_METHODS);
        assertThat(mapper.findByNameAndTenant("agent", null)).isNull();
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void shouldReturnNullWhenNameIsNull() {
        SfAgentMapper mapper = mock(SfAgentMapper.class, CALLS_REAL_METHODS);
        assertThat(mapper.findByNameAndTenant(null, "tenant")).isNull();
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void shouldQueryByNameAndTenant() {
        SfAgentMapper mapper = mock(SfAgentMapper.class, CALLS_REAL_METHODS);
        SfAgent expected = new SfAgent();
        expected.setId(1L);
        expected.setName("reviewer");
        doReturn(expected).when(mapper).selectOne(any(LambdaQueryWrapper.class));

        SfAgent result = mapper.findByNameAndTenant("reviewer", "tenant-a");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectOne(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void shouldReturnNullWhenNotFound() {
        SfAgentMapper mapper = mock(SfAgentMapper.class, CALLS_REAL_METHODS);
        doReturn(null).when(mapper).selectOne(any(LambdaQueryWrapper.class));

        assertThat(mapper.findByNameAndTenant("missing", "tenant-a")).isNull();
    }
}
