package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.entity.SfMcpServer;

public interface McpServerService extends IService<SfMcpServer> {

    boolean healthCheck(Long serverId);

    void validateEndpoint(String endpoint);
}
