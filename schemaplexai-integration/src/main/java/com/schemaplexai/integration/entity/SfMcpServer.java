package com.schemaplexai.integration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sf_mcp_server", autoResultMap = true)
public class SfMcpServer extends BaseEntity {

    private String name;
    private String endpoint;
    private String transport;
    private Integer status;

    /** Command for stdio transport (e.g., "npx", "node"). */
    private String command;

    /** Command arguments, serialized as JSON array in DB. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> args;

    /** Environment variables, serialized as JSON object in DB. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> envVars;

    /** Public key for server signature verification. */
    private String serverPublicKey;

    /** MCP protocol version (e.g., "2024-11-05"). */
    private String protocolVersion;

    /** Allowed tool names for this server, serialized as JSON array in DB. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> toolWhitelist;
}
