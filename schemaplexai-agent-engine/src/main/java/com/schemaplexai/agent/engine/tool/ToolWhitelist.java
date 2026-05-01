package com.schemaplexai.agent.engine.tool;

import java.util.Set;

/**
 * 工具白名单，控制允许执行的工具。
 */
public class ToolWhitelist {

    private final Set<String> allowedTools;

    public ToolWhitelist(Set<String> allowedTools) {
        this.allowedTools = Set.copyOf(allowedTools);
    }

    /**
     * 检查工具是否在白名单中
     * @param toolName 工具名
     * @return true 如果允许执行
     */
    public boolean isAllowed(String toolName) {
        return toolName != null && allowedTools.contains(toolName);
    }

    /**
     * 获取所有允许的工具名
     * @return 允许的工具名集合
     */
    public Set<String> getAllowedTools() {
        return allowedTools;
    }
}
