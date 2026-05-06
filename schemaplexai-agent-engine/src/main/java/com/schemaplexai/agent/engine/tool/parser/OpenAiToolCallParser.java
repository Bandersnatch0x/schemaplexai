package com.schemaplexai.agent.engine.tool.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses OpenAI tool_calls JSON format.
 * Extracts tool_calls array from assistant message content,
 * parsing function.name and function.arguments for each tool call.
 */
@Slf4j
@Component
public class OpenAiToolCallParser implements ToolCallParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public List<ToolCall> parse(String content, LlmProvider provider) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode toolCalls = root.get("tool_calls");

            if (toolCalls == null || !toolCalls.isArray()) {
                return Collections.emptyList();
            }

            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode node : toolCalls) {
                JsonNode function = node.get("function");
                if (function == null) continue;

                String name = function.get("name") != null ? function.get("name").asText() : null;
                if (name == null || name.isBlank()) continue;

                Map<String, Object> parameters = new HashMap<>();
                JsonNode arguments = function.get("arguments");
                if (arguments != null && !arguments.isNull()) {
                    if (arguments.isObject()) {
                        // Arguments is a JSON object node (some providers send this directly)
                        arguments.fields().forEachRemaining(
                                f -> parameters.put(f.getKey(), f.getValue().asText()));
                    } else if (arguments.isTextual()) {
                        // Arguments is a JSON string that needs to be parsed
                        String argsStr = arguments.asText();
                        if (!argsStr.isBlank()) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> parsed = objectMapper.readValue(argsStr, Map.class);
                                parameters.putAll(parsed);
                            } catch (Exception e) {
                                log.warn("Failed to parse tool call arguments string: {}", argsStr);
                            }
                        }
                    }
                }

                calls.add(new ToolCall(name, parameters));
            }

            return calls;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI tool_calls: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
