package com.schemaplexai.agent.engine.tool.parser;

/**
 * Thrown when structured tool-call parsing cannot be performed because no
 * parser is registered for the LLM provider that produced the response.
 *
 * <p>This is an explicit failure signal (issue 905 / REQ-02): the state machine
 * must fail loudly instead of silently treating the response as "no tool calls",
 * which previously caused an endless THINKING &rarr; TOOL_CALLING empty-spin that
 * only ended in loop detection.</p>
 */
public class ToolCallParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String providerName;

    public ToolCallParseException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }

    /**
     * The provider name that could not be routed to a parser
     * ({@code null} when the provider was unknown/unset).
     */
    public String getProviderName() {
        return providerName;
    }
}
