package com.schemaplexai.agent.engine.tool.parser;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Anthropic tool_use XML format.
 * Extracts tool_use blocks from assistant message content,
 * parsing the name attribute and nested parameter elements.
 */
@Slf4j
@Component
public class AnthropicToolCallParser implements ToolCallParser {

    private static final Pattern TOOL_USE_PATTERN = Pattern.compile(
            "<tool_use>(.*?)</tool_use>", Pattern.DOTALL);
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "<name>(.*?)</name>", Pattern.DOTALL);
    private static final Pattern PARAM_PATTERN = Pattern.compile(
            "<parameter\\s+name=\"([^\"]+)\">(.*?)</parameter>", Pattern.DOTALL);

    @Override
    public String getProviderName() {
        return "ANTHROPIC";
    }

    @Override
    public List<ToolCall> parse(String content, LlmProvider provider) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Matcher toolUseMatcher = TOOL_USE_PATTERN.matcher(content);
            List<ToolCall> calls = new ArrayList<>();

            while (toolUseMatcher.find()) {
                String toolUseBlock = toolUseMatcher.group(1);

                Matcher nameMatcher = NAME_PATTERN.matcher(toolUseBlock);
                if (!nameMatcher.find()) continue;
                String name = nameMatcher.group(1).trim();
                if (name.isBlank()) continue;

                Map<String, Object> parameters = new HashMap<>();
                Matcher paramMatcher = PARAM_PATTERN.matcher(toolUseBlock);
                while (paramMatcher.find()) {
                    String paramName = paramMatcher.group(1).trim();
                    String paramValue = paramMatcher.group(2).trim();
                    parameters.put(paramName, paramValue);
                }

                calls.add(new ToolCall(name, parameters));
            }

            return calls;
        } catch (Exception e) {
            log.warn("Failed to parse Anthropic tool_use XML: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
