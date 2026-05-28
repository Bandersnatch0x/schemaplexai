package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.lifecycle.PauseReason;
import com.schemaplexai.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Execution lifecycle web port")
class EngineExecutionLifecyclePortTest {

    @Mock
    private ObjectProvider<AgentExecutionLifecycleService> lifecycleServiceProvider;

    @Mock
    private AgentExecutionLifecycleService lifecycleService;

    @InjectMocks
    private EngineExecutionLifecyclePort port;

    @Test
    @DisplayName("pause delegates to engine lifecycle service with user-request reason")
    void pauseExecution_delegatesToEngineLifecycleService() {
        when(lifecycleServiceProvider.getIfAvailable()).thenReturn(lifecycleService);

        port.pauseExecution(1L);

        verify(lifecycleService).pauseExecution(1L, PauseReason.USER_REQUEST);
    }

    @Test
    @DisplayName("resume delegates to engine lifecycle service")
    void resumeExecution_delegatesToEngineLifecycleService() {
        when(lifecycleServiceProvider.getIfAvailable()).thenReturn(lifecycleService);

        port.resumeExecution(1L);

        verify(lifecycleService).resumeExecution(1L);
    }

    @Test
    @DisplayName("cancel delegates to engine lifecycle service")
    void cancelExecution_delegatesToEngineLifecycleService() {
        when(lifecycleServiceProvider.getIfAvailable()).thenReturn(lifecycleService);

        port.cancelExecution(1L);

        verify(lifecycleService).cancelExecution(1L);
    }

    @Test
    @DisplayName("missing engine lifecycle service fails explicitly")
    void missingEngineLifecycleService_throwsExplicitException() {
        when(lifecycleServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> port.pauseExecution(1L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Execution lifecycle service is not available");
    }
}
