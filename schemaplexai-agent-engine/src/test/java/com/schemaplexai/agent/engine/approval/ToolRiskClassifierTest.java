package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.tool.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolRiskClassifierTest {

    private ToolRiskClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ToolRiskClassifier();
    }

    @Test
    void shouldClassifyHighRiskTool() {
        ToolCall toolCall = new ToolCall("volumeDelete");
        assertEquals("HIGH", classifier.classify(toolCall, "tenant-1"));
    }

    @Test
    void shouldClassifyCriticalRiskTool() {
        ToolCall toolCall = new ToolCall("systemShutdown");
        assertEquals("CRITICAL", classifier.classify(toolCall, "tenant-1"));
    }

    @Test
    void shouldClassifyUnknownToolAsLow() {
        ToolCall toolCall = new ToolCall("fileRead");
        assertEquals("LOW", classifier.classify(toolCall, "tenant-1"));
    }

    @Test
    void shouldUseTenantOverride() {
        classifier.setTenantOverride("tenant-1", "fileRead", "HIGH");
        ToolCall toolCall = new ToolCall("fileRead");
        assertEquals("HIGH", classifier.classify(toolCall, "tenant-1"));
    }

    @Test
    void shouldNotUseOverrideForDifferentTenant() {
        classifier.setTenantOverride("tenant-1", "fileRead", "HIGH");
        ToolCall toolCall = new ToolCall("fileRead");
        assertEquals("LOW", classifier.classify(toolCall, "tenant-2"));
    }

    @Test
    void shouldRequireApprovalForHighRiskInManualMode() {
        ToolCall toolCall = new ToolCall("volumeDelete");
        assertTrue(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.MANUAL));
    }

    @Test
    void shouldNotRequireApprovalForHighRiskInAutoMode() {
        ToolCall toolCall = new ToolCall("volumeDelete");
        assertFalse(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.AUTO));
    }

    @Test
    void shouldNotRequireApprovalForHighRiskInAuditOnlyMode() {
        ToolCall toolCall = new ToolCall("volumeDelete");
        assertFalse(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.AUDIT_ONLY));
    }

    @Test
    void shouldAlwaysRequireApprovalForCriticalInManualMode() {
        ToolCall toolCall = new ToolCall("systemShutdown");
        assertTrue(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.MANUAL));
    }

    @Test
    void shouldNotRequireApprovalForLowRiskInAnyMode() {
        ToolCall toolCall = new ToolCall("fileRead");
        assertFalse(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.MANUAL));
        assertFalse(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.AUTO));
        assertFalse(classifier.requiresApproval(toolCall, "tenant-1", ApprovalMode.AUDIT_ONLY));
    }
}
