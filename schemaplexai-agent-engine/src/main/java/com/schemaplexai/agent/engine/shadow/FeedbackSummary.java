package com.schemaplexai.agent.engine.shadow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary of a single shadow feedback memory entry for an agent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSummary {

    private Long memoryId;
    private Long agentId;
    private Long sourceExecutionId;
    private FeedbackActionType actionType;
    private String content;
    private LocalDateTime createdAt;
}
