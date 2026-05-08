package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;

import java.util.List;
import java.util.Map;

public interface McpServerService extends IService<SfMcpServer> {

    boolean healthCheck(Long serverId);

    void validateEndpoint(String endpoint);

    List<McpToolSchema> discoverTools(Long serverId);

    String invokeTool(Long serverId, String toolName, Map<String, Object> arguments);
}
