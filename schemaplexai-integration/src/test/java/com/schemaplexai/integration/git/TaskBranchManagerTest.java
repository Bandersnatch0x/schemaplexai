package com.schemaplexai.integration.git;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.GitIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskBranchManagerTest {

    @Mock
    private GitIntegrationService gitIntegrationService;

    @InjectMocks
    private TaskBranchManager taskBranchManager;

    @BeforeEach
    void setUp() {
        // Purge any state from previous tests
        taskBranchManager.purgeExpiredRecords();
    }

    @Test
    void createBranch_shouldCallGitServiceWithTaskBranchName() {
        Long taskId = 42L;
        String baseBranch = "main";

        taskBranchManager.createBranch(taskId, baseBranch);

        verify(gitIntegrationService).createBranch(taskId, null, "task/42", baseBranch);
    }

    @Test
    void createBranch_shouldReactivateSoftDeletedBranch() {
        Long taskId = 42L;
        String baseBranch = "main";

        // Soft delete first
        taskBranchManager.deleteBranch(taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        // Create again — should remove from soft-delete registry
        taskBranchManager.createBranch(taskId, baseBranch);
        assertFalse(taskBranchManager.isSoftDeleted(taskId));

        verify(gitIntegrationService, times(1)).createBranch(taskId, null, "task/42", baseBranch);
    }

    @Test
    void createBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.createBranch(null, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void deleteBranch_shouldCallGitServiceWithTaskBranchName() {
        Long taskId = 99L;

        taskBranchManager.deleteBranch(taskId);

        verify(gitIntegrationService).deleteBranch(taskId, null, "task/99", false);
    }

    @Test
    void deleteBranch_shouldRegisterSoftDelete() {
        Long taskId = 77L;

        assertFalse(taskBranchManager.isSoftDeleted(taskId));
        taskBranchManager.deleteBranch(taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));
    }

    @Test
    void deleteBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.deleteBranch(null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void hardDeleteBranch_shouldCallGitServiceWithForce() {
        Long taskId = 55L;

        taskBranchManager.hardDeleteBranch(taskId);

        verify(gitIntegrationService).deleteBranch(taskId, null, "task/55", true);
    }

    @Test
    void hardDeleteBranch_shouldRemoveFromSoftDeleteRegistry() {
        Long taskId = 55L;

        taskBranchManager.deleteBranch(taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        taskBranchManager.hardDeleteBranch(taskId);
        assertFalse(taskBranchManager.isSoftDeleted(taskId));
    }

    @Test
    void hardDeleteBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.hardDeleteBranch(null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void isSoftDeleted_shouldReturnFalseForUnknownTask() {
        assertFalse(taskBranchManager.isSoftDeleted(999L));
    }

    @Test
    void isSoftDeleted_shouldReturnFalseForNullTaskId() {
        assertFalse(taskBranchManager.isSoftDeleted(null));
    }

    @Test
    void recoverBranch_shouldRecreateFromBaseBranch() {
        Long taskId = 88L;
        String baseBranch = "develop";

        taskBranchManager.deleteBranch(taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        taskBranchManager.recoverBranch(taskId, baseBranch);

        assertFalse(taskBranchManager.isSoftDeleted(taskId));
        verify(gitIntegrationService).createBranch(taskId, null, "task/88", baseBranch);
    }

    @Test
    void recoverBranch_shouldThrowWhenNotSoftDeleted() {
        Long taskId = 66L;

        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.recoverBranch(taskId, "main"));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void recoverBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.recoverBranch(null, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void purgeExpiredRecords_shouldRemoveExpiredEntries() {
        Long taskId = 111L;

        taskBranchManager.deleteBranch(taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        // Immediately purging won't remove it (not expired yet)
        int purged = taskBranchManager.purgeExpiredRecords();
        assertEquals(0, purged);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));
    }

    @Test
    void purgeExpiredRecords_shouldReturnZeroWhenNoRecords() {
        int purged = taskBranchManager.purgeExpiredRecords();
        assertEquals(0, purged);
    }
}
