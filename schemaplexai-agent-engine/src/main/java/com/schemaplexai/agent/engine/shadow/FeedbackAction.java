package com.schemaplexai.agent.engine.shadow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAction {

    private FeedbackActionType type;
    private String description;
    private String payload;
}
