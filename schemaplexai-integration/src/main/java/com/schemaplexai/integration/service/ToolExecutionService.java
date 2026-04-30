package com.schemaplexai.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutionService {

    private final List<ToolExecutor> executors;
    private final ObjectMapper objectMapper;

    private Map<String, ToolExecutor> executorRegistry;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.executorRegistry = executors.stream()
                .collect(Collectors.toMap(ToolExecutor::getToolName, Function.identity()));
    }

    public String executeTool(String toolName, String parametersJson) {
        log.info("Execute tool: {}, parameters: {}", toolName, parametersJson);

        ToolExecutor executor = executorRegistry.get(toolName);
        if (executor == null) {
            throw new BaseException(ResultCode.INTEGRATION_NOT_FOUND,
                    "Tool not found: " + toolName);
        }

        try {
            Map<String, Object> parameters = parseParameters(parametersJson);
            return executor.execute(parameters);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Tool execution failed: " + e.getMessage());
        }
    }

    private Map<String, Object> parseParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(parametersJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse parameters JSON, using raw string", e);
            return Map.of("raw", parametersJson);
        }
    }
}
