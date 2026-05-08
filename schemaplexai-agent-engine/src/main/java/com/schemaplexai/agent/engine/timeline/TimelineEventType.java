package com.schemaplexai.agent.engine.timeline;

/**
 * Standard event types for the agent execution timeline.
 * Each type maps to a distinct visual card style on the frontend.
 */
public enum TimelineEventType {

    STATE_TRANSITION("state_transition"),
    THOUGHT("thought"),
    TOOL_CALL("tool_call"),
    TOOL_RESULT("tool_result"),
    APPROVAL_REQ("approval_req"),
    APPROVAL_RESP("approval_resp"),
    PLAN("plan"),
    FILE_DIFF("file_diff"),
    OUTPUT("output"),
    ERROR("error"),
    COMPLETED("completed");

    private final String value;

    TimelineEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
