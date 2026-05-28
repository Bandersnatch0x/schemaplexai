package com.schemaplexai.agent.engine.tool.mcp;

import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import com.schemaplexai.integration.mapper.McpServerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class McpToolDiscoveryConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(McpServerMapper.class, () -> mock(McpServerMapper.class))
            .withBean(McpClientManager.class, () -> mock(McpClientManager.class))
            .withBean(ToolRegistry.class, () -> mock(ToolRegistry.class))
            .withBean(GuardrailsEngine.class, () -> mock(GuardrailsEngine.class))
            .withUserConfiguration(McpToolDiscoveryService.class);

    @Test
    void createsMcpToolDiscoveryServiceByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(McpToolDiscoveryService.class)).hasSize(1));
    }

    @Test
    void skipsMcpToolDiscoveryServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("mcp.discovery.enabled=false")
                .run(context ->
                        assertThat(context.getBeansOfType(McpToolDiscoveryService.class)).isEmpty());
    }
}
