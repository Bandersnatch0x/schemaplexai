package com.schemaplexai.agent.engine.lifecycle;

import com.schemaplexai.agent.engine.state.AgentExecutionState;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExecutionSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long executionId;
    private AgentExecutionState state;
    private PauseReason pauseReason;
    private List<Map<String, Object>> chatHistory;
    private Map<String, Object> contextVariables;
    private Long consumedInputTokens;
    private Long consumedOutputTokens;
    private Integer currentRound;
    private LocalDateTime createdAt;
}
