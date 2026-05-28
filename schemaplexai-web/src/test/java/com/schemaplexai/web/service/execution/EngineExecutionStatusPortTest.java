package com.schemaplexai.web.service.execution;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.common.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.Map;

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

    @InjectMocks
    private EngineExecutionStatusPort port;

    @Test
    @DisplayName("status query reads engine execution and maps stable fields")
    void getExecutionStatus_mapsEngineExecutionFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 21, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 21, 9, 5);
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 21, 9, 10);
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(42L);
        execution.setTenantId("tenant-1");
        execution.setAgentId(7L);
        execution.setConversationId("conv-1");
        execution.setState("PAUSED");
        execution.setSnapshotId(100L);
        execution.setSkillName("planner");
        execution.setRoleName("architect");
        execution.setVersion(3);
        execution.setLastEventSeq(12);
        execution.setCreatedAt(createdAt);
        execution.setUpdatedAt(updatedAt);
        execution.setCompletedAt(completedAt);
        when(executionMapperProvider.getIfAvailable()).thenReturn(executionMapper);
        when(executionMapper.selectById(42L)).thenReturn(execution);

        Map<String, Object> status = port.getExecutionStatus(42L);

        assertThat(status)
                .containsEntry("executionId", 42L)
                .containsEntry("tenantId", "tenant-1")
                .containsEntry("agentId", 7L)
                .containsEntry("conversationId", "conv-1")
                .containsEntry("status", "PAUSED")
                .containsEntry("snapshotId", 100L)
                .containsEntry("skillName", "planner")
                .containsEntry("roleName", "architect")
                .containsEntry("version", 3)
                .containsEntry("lastEventSeq", 12)
                .containsEntry("createdAt", createdAt)
                .containsEntry("updatedAt", updatedAt)
                .containsEntry("completedAt", completedAt);
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
