package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.web.mapper.ExecutionMapper;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Execution status web port")
class EngineExecutionStatusPortTest {

    @Mock
    private ObjectProvider<SfAgentExecutionMapper> executionMapperProvider;

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private ExecutionMapper webExecutionMapper;

    @InjectMocks
    private EngineExecutionStatusPort port;

    @Test
    @DisplayName("status query reads engine execution and maps to typed VO")
    void getExecutionStatus_mapsEngineExecutionFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 21, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 21, 9, 5);
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(42L);
        execution.setTenantId("tenant-1");
        execution.setAgentId(7L);
        execution.setState("PAUSED");
        execution.setCreatedAt(createdAt);
        execution.setUpdatedAt(updatedAt);

        ExecutionStatusVO expected = new ExecutionStatusVO();
        expected.setExecutionId(42L);
        expected.setAgentId(7L);
        expected.setState("PAUSED");
        expected.setCreatedAt(createdAt);
        expected.setUpdatedAt(updatedAt);

        when(executionMapperProvider.getIfAvailable()).thenReturn(executionMapper);
        when(executionMapper.selectById(42L)).thenReturn(execution);
        when(webExecutionMapper.toStatusVO(execution)).thenReturn(expected);

        ExecutionStatusVO status = port.getExecutionStatus(42L);

        assertThat(status.getExecutionId()).isEqualTo(42L);
        assertThat(status.getAgentId()).isEqualTo(7L);
        assertThat(status.getState()).isEqualTo("PAUSED");
        assertThat(status.getCreatedAt()).isEqualTo(createdAt);
        assertThat(status.getUpdatedAt()).isEqualTo(updatedAt);
        verify(executionMapper).selectById(42L);
    }

    @Test
    @DisplayName("missing engine execution mapper fails explicitly")
    void missingExecutionMapper_throwsExplicitException() {
        when(executionMapperProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> port.getExecutionStatus(42L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Execution status service is not available");
    }

    @Test
    @DisplayName("invalid execution id fails with parameter error")
    void invalidExecutionId_throwsParamError() {
        assertThatThrownBy(() -> port.getExecutionStatus(0L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("unknown execution id fails with not found")
    void unknownExecutionId_throwsNotFound() {
        when(executionMapperProvider.getIfAvailable()).thenReturn(executionMapper);
        when(executionMapper.selectById(42L)).thenReturn(null);

        assertThatThrownBy(() -> port.getExecutionStatus(42L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(404);
    }
}
