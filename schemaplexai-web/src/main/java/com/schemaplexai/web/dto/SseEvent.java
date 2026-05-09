package com.schemaplexai.web.dto;

public record SseEvent(
        Long executionId,
        int seq,
        String eventType,
        String payload,
        String sensitivity
) {
}
