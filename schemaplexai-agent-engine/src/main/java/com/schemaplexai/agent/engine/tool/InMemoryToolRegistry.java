package com.schemaplexai.agent.engine.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的线程安全工具注册表实现。
 */
@Slf4j
@Component
public class InMemoryToolRegistry implements ToolRegistry {

    private final ConcurrentHashMap<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    @Override
    public void register(ToolDefinition definition) {
        if (definition == null || definition.name() == null || definition.name().isBlank()) {
            throw new IllegalArgumentException("Tool definition name must not be blank");
        }
        ToolDefinition existing = tools.putIfAbsent(definition.name(), definition);
        if (existing != null) {
            throw new IllegalArgumentException("Tool already registered: " + definition.name());
        }
        log.info("Tool registered: {} ({} parameters)", definition.name(), definition.parameters().size());
    }

    @Override
    public void registerAll(List<ToolDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (ToolDefinition def : definitions) {
            register(def);
        }
    }

    @Override
    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    @Override
    public List<ToolDefinition> getAll() {
        return new ArrayList<>(tools.values());
    }

    @Override
    public List<Map<String, Object>> getAllAsOpenAiFunctions() {
        List<Map<String, Object>> functions = new ArrayList<>();
        for (ToolDefinition def : tools.values()) {
            functions.add(def.toOpenAiFunction());
        }
        return functions;
    }

    @Override
    public List<Map<String, Object>> getAllAsAnthropicTools() {
        List<Map<String, Object>> toolSchemas = new ArrayList<>();
        for (ToolDefinition def : tools.values()) {
            toolSchemas.add(def.toAnthropicTool());
        }
        return toolSchemas;
    }

    @Override
    public boolean exists(String name) {
        return tools.containsKey(name);
    }

    @Override
    public ToolDefinition unregister(String name) {
        ToolDefinition removed = tools.remove(name);
        if (removed != null) {
            log.info("Tool unregistered: {}", name);
        }
        return removed;
    }
}
