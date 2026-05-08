package com.schemaplexai.integration.git;

import com.schemaplexai.integration.service.GitIntegrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TaskBranchManagerTest {

    @Mock
    private GitIntegrationService gitIntegrationService;

    @InjectMocks
    private TaskBranchManager taskBranchManager;

    @Test
    void createBranch_shouldCallGitServiceWithTaskBranchName() {
        Long taskId = 42L;
        String baseBranch = "main";

        taskBranchManager.createBranch(taskId, baseBranch);

        verify(gitIntegrationService).createBranch(taskId, null, "task/42", baseBranch);
    }

    @Test
    void deleteBranch_shouldCallGitServiceWithTaskBranchName() {
        Long taskId = 99L;

        taskBranchManager.deleteBranch(taskId);

        verify(gitIntegrationService).deleteBranch(taskId, null, "task/99", false);
    }

    @Test
    void createBranch_shouldThrowWhenTaskIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskBranchManager.createBranch(null, "main"));
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void deleteBranch_shouldThrowWhenTaskIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> taskBranchManager.deleteBranch(null));
        verifyNoInteractions(gitIntegrationService);
    }
}
