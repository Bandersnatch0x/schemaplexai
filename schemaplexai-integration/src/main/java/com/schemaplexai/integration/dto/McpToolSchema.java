package com.schemaplexai.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpToolSchema {

    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
