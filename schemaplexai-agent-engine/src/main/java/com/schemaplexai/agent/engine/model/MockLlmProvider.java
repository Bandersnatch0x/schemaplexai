package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock LLM provider for demo/testing — returns deterministic ReAct-formatted responses.
 *
 * <p>Activated via {@code spring.profiles.active=mock} or {@code llm.provider=mock}.
 * No API key or external connectivity required.
 *
 * <p>Cycles through three sample responses that showcase platform capabilities:
 * <ol>
 *   <li>Architecture analysis with ReAct tool invocation</li>
 *   <li>Code review with ReAct tool invocation</li>
 *   <li>Direct final answer — agent capabilities overview</li>
 * </ol>
 */
@Slf4j
@Component
@Profile("mock")
public class MockLlmProvider implements LlmProvider {

    private static final String PROVIDER_NAME = "MOCK";

    private final AtomicInteger counter = new AtomicInteger(0);

    // --- Demo response pool ---------------------------------------------------

    private static final String DEMO_RESPONSE_1 = """
            Thought: The user wants to analyze the architecture of the project. I should use the codebase search tool to gather information about the module structure.
            Action: codebase_search
            Action Input: {"query": "module dependencies and architecture", "scope": "project"}
            """;

    private static final String DEMO_RESPONSE_2 = """
            Thought: The user requested a code review. I need to examine the relevant source files and check for quality, security, and best practices.
            Action: read_file
            Action Input: {"path": "src/main/java/com/schemaplexai/agent/engine/model/OpenAiProvider.java"}
            """;

    private static final String DEMO_RESPONSE_3 = """
            Final Answer: SchemaPlexAI is an Enterprise AI R&D collaboration platform with the following key capabilities:

            1. **Agent Orchestration** — Multi-step AI agent execution with ReAct reasoning, tool calling, and iterative refinement. Supports configurable LLM providers (OpenAI, Anthropic) with automatic fallback routing.

            2. **Spec-Driven Workflow** — From specification documents through BPMN workflow execution (Flowable), with AI-assisted review gates and quality checkpoints.

            3. **Context-Aware RAG** — Vector search powered by Milvus for semantic code retrieval, document ingestion, and knowledge graph queries.

            4. **Multi-Tenant Architecture** — Row-level tenant isolation via interceptor-based filtering, with JWT authentication and RBAC permissions.

            5. **Observability Stack** — Integrated Prometheus metrics, Grafana dashboards, ELK log aggregation, and Jaeger distributed tracing.

            The platform is built on Spring Boot 3.2 + Java 21 (backend) and React 18 + TypeScript + Ant Design (frontend).
            """;

    // --- LlmProvider implementation -------------------------------------------

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.debug("Mock generate called: model={}, temp={}", modelId, temperature);
        return nextDemoResponse();
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.debug("Mock generateWithMessages called: model={}, messageCount={}, temp={}",
                modelId, messages != null ? messages.size() : 0, temperature);
        return nextDemoResponse();
    }

    @Override
    public String generateWithTools(List<LlmMessage> messages, List<ToolDefinition> tools,
                                    String modelId, Double temperature) {
        log.debug("Mock generateWithTools called: model={}, messageCount={}, toolCount={}, temp={}",
                modelId, messages != null ? messages.size() : 0,
                tools != null ? tools.size() : 0, temperature);
        return nextDemoResponse();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    // --- Internal -------------------------------------------------------------

    /**
     * Returns the next demo response in round-robin order.
     * Responses 1 and 2 contain ReAct tool-call formatting;
     * response 3 is a direct Final Answer.
     */
    private String nextDemoResponse() {
        int index = counter.getAndIncrement() % 3;
        return switch (index) {
            case 0 -> DEMO_RESPONSE_1;
            case 1 -> DEMO_RESPONSE_2;
            default -> DEMO_RESPONSE_3;
        };
    }
}
