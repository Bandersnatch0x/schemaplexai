package com.schemaplexai.agent.engine.tool.subagent;

import java.util.Map;

/**
 * Request to execute a sub-agent task.
 *
 * @param parentExecutionId   the parent execution that initiated this sub-agent
 * @param prompt              the user prompt / task description for the sub-agent
 * @param role                the role assigned to the sub-agent (e.g. "subagent", "analyzer")
 * @param inheritedGuardrails guardrails inherited from the parent execution context
 * @param maxDepth            remaining depth budget for nested sub-agent calls
 */
public record SubAgentRequest(
    Long parentExecutionId,
    String prompt,
    String role,
    Map<String, Object> inheritedGuardrails,
    int maxDepth
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long parentExecutionId;
        private String prompt;
        private String role = "subagent";
        private Map<String, Object> inheritedGuardrails = Map.of();
        private int maxDepth = 3;

        public Builder parentExecutionId(Long parentExecutionId) {
            this.parentExecutionId = parentExecutionId;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder inheritedGuardrails(Map<String, Object> inheritedGuardrails) {
            this.inheritedGuardrails = inheritedGuardrails != null ? inheritedGuardrails : Map.of();
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public SubAgentRequest build() {
            if (parentExecutionId == null) {
                throw new IllegalArgumentException("parentExecutionId is required");
            }
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("prompt is required");
            }
            return new SubAgentRequest(parentExecutionId, prompt, role, inheritedGuardrails, maxDepth);
        }
    }
}
