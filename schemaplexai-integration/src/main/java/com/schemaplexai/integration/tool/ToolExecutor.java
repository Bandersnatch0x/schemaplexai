package com.schemaplexai.integration.tool;

import java.util.Map;

public interface ToolExecutor {

    String getToolName();

    String execute(Map<String, Object> parameters);
}
