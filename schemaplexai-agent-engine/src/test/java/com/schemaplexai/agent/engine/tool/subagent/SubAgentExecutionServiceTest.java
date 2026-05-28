package com.schemaplexai.agent.engine.tool.subagent;

import com.schemaplexai.agent.engine.AgentExecutionRunner;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubAgentExecutionServiceTest {

    @Mock
    private ObjectProvider<AgentExecutionRunner> executionRunnerProvider;

    @Mock
    private AgentExecutionRunner executionRunner;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    private SubAgentExecutionService service;

    @BeforeEach
    void setUp() {
        service = new SubAgentExecutionService(executionRunnerProvider, chatMemoryStore);
    }

    @Test
    void executeResolvesRunnerOnlyWhenDispatchingChildExecution() {
        when(executionRunnerProvider.getObject()).thenReturn(executionRunner);
        SubAgentRequest request = SubAgentRequest.builder()
                .parentExecutionId(10L)
                .prompt("summarize the design")
                .role("analyst")
                .maxDepth(2)
                .build();

        SubAgentResult result = service.execute(request);

        ArgumentCaptor<SfAgentExecution> executionCaptor = ArgumentCaptor.forClass(SfAgentExecution.class);
        verify(chatMemoryStore).saveMessage(any(), eq(new LlmMessage("user", "summarize the design")));
        verify(executionRunnerProvider).getObject();
        verify(executionRunner).runExecutionAsync(executionCaptor.capture(), eq(null), eq("summarize the design"));
        SfAgentExecution childExecution = executionCaptor.getValue();
        assertThat(childExecution.getMetadata("parentExecutionId")).isEqualTo(10L);
        assertThat(childExecution.getMetadata("role")).isEqualTo("analyst");
        assertThat(childExecution.getMetadata("maxDepth")).isEqualTo(2);
        assertThat(result.output()).contains("Sub-agent execution started");
        assertThat(result.executionId()).isEqualTo(childExecution.getId());
    }
}
