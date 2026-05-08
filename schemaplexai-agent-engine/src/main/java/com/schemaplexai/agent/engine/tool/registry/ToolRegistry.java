package com.schemaplexai.agent.engine.tool.registry;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.agent.engine.tool.parser.ToolCallParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central tool registry for discovery, resolution, and parsing.
 * Sits upstream of ToolSandbox — handles registration/discovery/parsing,
 * while ToolSandbox handles sandboxed execution safety.
 *
 * <p>Call chain: ToolCallingStateHandler → ToolRegistry.resolve() → ToolSandbox.execute()</p>
 *
 * <p>Each tool MUST pass ToolSafetyGuard.check() before execution.</p>
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, ToolAdapter> adapters = new ConcurrentHashMap<>();
    private final Map<String, ToolCallParser> parsers = new ConcurrentHashMap<>();

    /**
     * Auto-discover all ToolAdapter and ToolCallParser Spring beans.
     */
    public ToolRegistry(List<ToolAdapter> adapterList, List<ToolCallParser> parserList) {
        for (ToolAdapter adapter : adapterList) {
            register(adapter);
        }
        for (ToolCallParser parser : parserList) {
            parsers.put(parser.getProviderName(), parser);
        }
        log.info("ToolRegistry initialized with {} adapters and {} parsers",
                adapters.size(), parsers.size());
    }

    /**
     * Register a tool adapter. Overwrites existing adapter with same tool name.
     */
    public void register(ToolAdapter adapter) {
        String name = adapter.getToolName();
        adapters.put(name, adapter);
        log.info("Registered tool adapter: {}", name);
    }

    /**
     * Resolve a tool name to its adapter.
     *
     * @param toolName the tool name to look up
     * @return the matching ToolAdapter, or null if unregistered (whitelist violation)
     */
    public ToolAdapter resolve(String toolName) {
        return adapters.get(toolName);
    }

    /**
     * Check if a tool is registered (whitelist check).
     */
    public boolean isRegistered(String toolName) {
        return adapters.containsKey(toolName);
    }

    /**
     * Parse tool calls from an LLM response.
     * Routes to the appropriate parser based on LLM provider.
     *
     * @param content  the raw LLM response content
     * @param provider the LLM provider that generated the response
     * @return list of parsed tool calls, or empty list if no parser/parse error
     */
    public List<ToolCall> parse(String content, LlmProvider provider) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        String providerName = provider != null ? provider.getProviderName() : "GENERIC";
        ToolCallParser parser = parsers.get(providerName);

        if (parser == null) {
            log.warn("No parser registered for provider: {}, attempting generic parse", providerName);
            return Collections.emptyList();
        }

        try {
            List<ToolCall> calls = parser.parse(content, provider);
            // Filter out calls for unregistered tools (whitelist enforcement)
            List<ToolCall> validCalls = new ArrayList<>();
            for (ToolCall call : calls) {
                if (isRegistered(call.toolName())) {
                    validCalls.add(call);
                } else {
                    log.warn("Tool '{}' is not registered, skipping (whitelist violation)", call.toolName());
                }
            }
            return validCalls;
        } catch (Exception e) {
            log.error("Failed to parse tool calls for provider {}: {} ({})",
                    providerName, e.getClass().getSimpleName(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Return an unmodifiable view of registered tool names (for diagnostics/metrics).
     */
    public List<String> getRegisteredToolNames() {
        return List.copyOf(adapters.keySet());
    }

    /**
     * Return the number of registered adapters.
     */
    public int getAdapterCount() {
        return adapters.size();
    }
}
