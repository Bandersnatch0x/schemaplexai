package com.schemaplexai.workflow.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Deploys BPMN process definitions on application startup and provides
 * runtime operations for listing deployed processes and starting instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDeployService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    private static final String BPMN_PATTERN = "classpath:processes/*.bpmn20.xml";

    /**
     * Auto-deploys all BPMN files from classpath:processes/ on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void deployOnStartup() {
        log.info("[WorkflowDeploy] Starting BPMN deployment scan...");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(BPMN_PATTERN);

            if (resources.length == 0) {
                log.warn("[WorkflowDeploy] No BPMN files found at {}", BPMN_PATTERN);
                return;
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                deployResource(resource, filename);
            }

            log.info("[WorkflowDeploy] Deployment complete. {} process definition(s) active.",
                    countActiveProcessDefinitions());
        } catch (IOException e) {
            log.error("[WorkflowDeploy] Failed to scan BPMN resources", e);
            throw new BaseException(ResultCode.ERROR, "BPMN deployment failed: " + e.getMessage());
        }
    }

    private void deployResource(Resource resource, String filename) {
        try {
            // Check if already deployed with same resource name to avoid duplicate deployments
            List<Deployment> existingDeployments = repositoryService.createDeploymentQuery()
                    .deploymentName(filename)
                    .list();

            if (!existingDeployments.isEmpty()) {
                log.info("[WorkflowDeploy] Skipping {} — already deployed (deploymentId={})",
                        filename, existingDeployments.get(0).getId());
                return;
            }

            Deployment deployment = repositoryService.createDeployment()
                    .name(filename)
                    .addInputStream(filename, resource.getInputStream())
                    .deploy();

            List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .list();

            for (ProcessDefinition def : definitions) {
                log.info("[WorkflowDeploy] Deployed {} — key={}, version={}, deploymentId={}",
                        filename, def.getKey(), def.getVersion(), deployment.getId());
            }
        } catch (IOException e) {
            log.error("[WorkflowDeploy] Failed to deploy {}", filename, e);
            throw new BaseException(ResultCode.ERROR,
                    "Failed to deploy " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Lists all deployed (active) process definitions.
     */
    public List<ProcessDefinitionInfo> listDeployedProcesses() {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .active()
                .orderByProcessDefinitionKey()
                .asc()
                .list();

        return definitions.stream()
                .map(def -> new ProcessDefinitionInfo(
                        def.getId(),
                        def.getKey(),
                        def.getName(),
                        def.getVersion(),
                        def.getDeploymentId(),
                        def.isSuspended()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Starts a new process instance by process definition key.
     *
     * @param processDefinitionKey the BPMN process key (e.g., "specReviewApproval")
     * @param businessKey          optional business key for correlation
     * @param variables            process variables
     * @return the started process instance ID
     */
    public String startProcessInstance(String processDefinitionKey, String businessKey,
                                        Map<String, Object> variables) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .latestVersion()
                .singleResult();

        if (definition == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Process definition not found: " + processDefinitionKey);
        }

        ProcessInstance instance;
        if (businessKey != null && !businessKey.isBlank()) {
            instance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
        } else {
            instance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
        }

        log.info("[WorkflowDeploy] Started process instance id={}, key={}, businessKey={}",
                instance.getId(), processDefinitionKey, businessKey);
        return instance.getId();
    }

    /**
     * Suspends a process definition by key.
     */
    public void suspendProcessDefinition(String processDefinitionKey) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .latestVersion()
                .singleResult();

        if (definition == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Active process definition not found: " + processDefinitionKey);
        }

        repositoryService.suspendProcessDefinitionById(definition.getId());
        log.info("[WorkflowDeploy] Suspended process definition key={}", processDefinitionKey);
    }

    /**
     * Activates a suspended process definition by key.
     */
    public void activateProcessDefinition(String processDefinitionKey) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .suspended()
                .latestVersion()
                .singleResult();

        if (definition == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Suspended process definition not found: " + processDefinitionKey);
        }

        repositoryService.activateProcessDefinitionById(definition.getId());
        log.info("[WorkflowDeploy] Activated process definition key={}", processDefinitionKey);
    }

    private long countActiveProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery().active().count();
    }

    /**
     * DTO for process definition information.
     */
    public record ProcessDefinitionInfo(
            String id,
            String key,
            String name,
            int version,
            String deploymentId,
            boolean suspended
    ) {
    }
}
