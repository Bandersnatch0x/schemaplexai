package com.schemaplexai.integration.git;

import com.schemaplexai.integration.service.GitIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBranchManager {

    private final GitIntegrationService gitIntegrationService;

    public void createBranch(Long taskId, String baseBranch) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        String branchName = "task/" + taskId;
        gitIntegrationService.createBranch(taskId, null, branchName, baseBranch);
        log.info("Task branch created: {} from base: {}", branchName, baseBranch);
    }

    public void deleteBranch(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        String branchName = "task/" + taskId;
        gitIntegrationService.deleteBranch(taskId, null, branchName, false);
        log.info("Task branch deleted: {}", branchName);
    }
}
