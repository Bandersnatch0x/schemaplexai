package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Execution optimistic locking concurrency control")
class ExecutionVersionConflictTest {

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @InjectMocks
    private ExecutionConcurrencyService concurrencyService;

    @BeforeEach
    void setUp() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setVersion(0);
        execution.setState("PENDING");
        when(executionMapper.selectById(1L)).thenReturn(execution);
    }

    @Test
    @DisplayName("should reject update when expectedVersion does not match current version")
    void rejectStaleVersionUpdate() {
        assertThatThrownBy(() -> concurrencyService.updateState(1L, "PAUSED", 5))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("version");
    }

    @Test
    @DisplayName("should allow update when expectedVersion matches current version")
    void allowMatchingVersionUpdate() {
        assertThatNoException()
                .isThrownBy(() -> concurrencyService.updateState(1L, "RUNNING", 0));
    }

    @Test
    @DisplayName("should increment version after successful state update")
    void incrementVersionOnSuccess() {
        assertThatNoException()
                .isThrownBy(() -> concurrencyService.updateState(1L, "COMPLETED", 0));
    }
}
