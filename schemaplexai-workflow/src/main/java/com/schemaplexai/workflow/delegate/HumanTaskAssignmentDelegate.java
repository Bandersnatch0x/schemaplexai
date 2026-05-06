package com.schemaplexai.workflow.delegate;

import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Task listener for human task assignment in BPMN workflows.
 * Logs assignment events and enriches task variables with tenant context.
 */
@Slf4j
@Component
public class HumanTaskAssignmentDelegate implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String eventName = delegateTask.getEventName();

        // Resolve tenantId from task variables or context
        String tenantId = (String) delegateTask.getVariable("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }
            delegateTask.setVariable("tenantId", tenantId);
        }
        TenantContextHolder.setTenantId(tenantId);

        // Build assignment metadata
        Map<String, Object> assignmentMeta = new HashMap<>();
        assignmentMeta.put("taskId", taskId);
        assignmentMeta.put("taskName", taskName);
        assignmentMeta.put("processInstanceId", processInstanceId);
        assignmentMeta.put("tenantId", tenantId);
        assignmentMeta.put("eventName", eventName);
        assignmentMeta.put("assignedAt", Instant.now().toString());

        switch (eventName) {
            case TaskListener.EVENTNAME_CREATE -> {
                String assignee = delegateTask.getAssignee();
                String candidateGroup = delegateTask.getCandidates().isEmpty()
                        ? null
                        : delegateTask.getCandidates().iterator().next().getGroupId();

                assignmentMeta.put("assignee", assignee);
                assignmentMeta.put("candidateGroup", candidateGroup);

                // Store assignment info for querying
                delegateTask.setVariableLocal("assignmentMeta", assignmentMeta);
                delegateTask.setVariable("lastTaskAssigned", taskId);

                log.info("[HumanTask] Created taskId={}, name={}, assignee={}, group={}, tenant={}, process={}",
                        taskId, taskName, assignee, candidateGroup, tenantId, processInstanceId);
            }
            case TaskListener.EVENTNAME_ASSIGNMENT -> {
                String newAssignee = delegateTask.getAssignee();
                assignmentMeta.put("newAssignee", newAssignee);
                delegateTask.setVariableLocal("assignmentMeta", assignmentMeta);
                log.info("[HumanTask] Assigned taskId={}, name={}, newAssignee={}, tenant={}, process={}",
                        taskId, taskName, newAssignee, tenantId, processInstanceId);
            }
            case TaskListener.EVENTNAME_COMPLETE -> {
                assignmentMeta.put("completedAt", Instant.now().toString());
                delegateTask.setVariable("lastTaskCompleted", taskId);
                log.info("[HumanTask] Completed taskId={}, name={}, tenant={}, process={}",
                        taskId, taskName, tenantId, processInstanceId);
            }
            case TaskListener.EVENTNAME_DELETE -> {
                log.info("[HumanTask] Deleted taskId={}, name={}, tenant={}, process={}",
                        taskId, taskName, tenantId, processInstanceId);
            }
            default -> {
                log.debug("[HumanTask] Event {} for taskId={}, name={}", eventName, taskId, taskName);
            }
        }
    }
}
