package com.schemaplexai.quality.service;

import com.schemaplexai.quality.entity.ApprovalTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge between the quality approval domain and the workflow BPMN engine.
 *
 * <p>Quality owns ApprovalTicket state. Workflow owns BPMN process execution.
 * This bridge starts BPMN instances and signals escalation via REST calls to the workflow service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowBridge {

    private final RestTemplate restTemplate;

    @Value("${workflow.service.url:http://localhost:8087}")
    private String workflowServiceUrl;

    /**
     * Starts a BPMN approval workflow for the given ticket.
     *
     * @param ticket the approval ticket (must be PENDING_BPMN)
     * @return the BPMN process instance ID
     */
    public String startApprovalWorkflow(ApprovalTicket ticket) {
        String url = workflowServiceUrl + "/workflow/deploy/start";

        Map<String, Object> request = new HashMap<>();
        request.put("processDefinitionKey", "agentExecutionApproval");
        request.put("businessKey", ticket.getTicketId().toString());

        Map<String, Object> variables = new HashMap<>();
        variables.put("ticketId", ticket.getTicketId().toString());
        variables.put("executionId", ticket.getExecutionId().toString());
        variables.put("tenantId", ticket.getTenantId().toString());
        variables.put("agentId", ticket.getAgentId() != null ? ticket.getAgentId().toString() : "");
        variables.put("riskLevel", ticket.getRiskLevel() != null ? ticket.getRiskLevel() : "HIGH");
        variables.put("actionDescription", ticket.getActionDescription() != null ? ticket.getActionDescription() : "");
        variables.put("approverId", "approver-group");
        variables.put("decisionVersion", "0");
        variables.put("expectedExecutionVersion",
                ticket.getExpectedExecutionVersion() != null ? ticket.getExpectedExecutionVersion().toString() : "0");
        request.put("variables", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            String processInstanceId = restTemplate.postForObject(url, entity, String.class);
            log.info("[ApprovalBridge] Started BPMN workflow instance={} for ticket={}",
                    processInstanceId, ticket.getTicketId());
            return processInstanceId;
        } catch (Exception e) {
            log.error("[ApprovalBridge] Failed to start BPMN workflow for ticket={}", ticket.getTicketId(), e);
            throw new RuntimeException("Failed to start approval workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Signals escalation within an existing BPMN workflow.
     *
     * @param ticket     the approval ticket
     * @param approverId the approver requesting escalation
     * @param reason     escalation reason
     */
    public void signalEscalation(ApprovalTicket ticket, String approverId, String reason) {
        // Complete the current human task with ESCALATE decision
        // The BPMN process will route to the escalation path
        String url = workflowServiceUrl + "/workflow/instances/" + ticket.getTicketId() + "/signal";

        Map<String, Object> signal = new HashMap<>();
        signal.put("approvalDecision", "ESCALATE");
        signal.put("approverId", approverId);
        signal.put("escalationReason", reason);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(signal, headers);

        try {
            restTemplate.postForObject(url, entity, Void.class);
            log.info("[ApprovalBridge] Escalation signal sent for ticket={}", ticket.getTicketId());
        } catch (Exception e) {
            log.error("[ApprovalBridge] Failed to signal escalation for ticket={}", ticket.getTicketId(), e);
            throw new RuntimeException("Failed to signal escalation: " + e.getMessage(), e);
        }
    }
}
