package com.schemaplexai.integration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, SfMcpServer> implements McpServerService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean healthCheck(Long serverId) {
        SfMcpServer server = baseMapper.selectById(serverId);
        if (server == null || server.getEndpoint() == null) {
            return false;
        }

        try {
            restTemplate.getForObject(server.getEndpoint() + "/health", String.class);
            log.info("MCP server {} health check passed", serverId);
            return true;
        } catch (ResourceAccessException e) {
            log.warn("MCP server {} health check failed: connection refused", serverId);
            return false;
        } catch (Exception e) {
            log.warn("MCP server {} health check failed: {}", serverId, e.getMessage());
            return false;
        }
    }

    @Override
    public void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "MCP endpoint is required");
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            throw new BaseException(ResultCode.PARAM_ERROR, "MCP endpoint must start with http:// or https://");
        }
    }
}
