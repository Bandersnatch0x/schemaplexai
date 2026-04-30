package com.schemaplexai.common.message;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnifiedMessage {
    private MessageType type;
    private String source;
    private String target;
    private String eventName;
    private String payload;
    private Long timestamp;
    private String traceId;
}
