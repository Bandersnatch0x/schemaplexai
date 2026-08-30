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

    private static final Long TENANT_ID = 1L;

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

        taskBranchManager.createBranch(TENANT_ID, taskId, baseBranch);

        verify(gitIntegrationService).createBranch(TENANT_ID, taskId, null, "task/42", baseBranch);
    }

    @Test
    void createBranch_shouldReactivateSoftDeletedBranch() {
        Long taskId = 42L;
        String baseBranch = "main";

        // Soft delete first
        taskBranchManager.deleteBranch(TENANT_ID, taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        // Create again — should remove from soft-delete registry
        taskBranchManager.createBranch(TENANT_ID, taskId, baseBranch);
        assertFalse(taskBranchManager.isSoftDeleted(taskId));

        verify(gitIntegrationService, times(1)).createBranch(TENANT_ID, taskId, null, "task/42", baseBranch);
    }

    @Test
    void createBranch_shouldThrowWhenTenantIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.createBranch(null, 42L, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void createBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.createBranch(TENANT_ID, null, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void deleteBranch_shouldCallGitServiceWithTaskBranchName() {
        Long taskId = 99L;

        taskBranchManager.deleteBranch(TENANT_ID, taskId);

        verify(gitIntegrationService).deleteBranch(TENANT_ID, taskId, null, "task/99", false);
    }

    @Test
    void deleteBranch_shouldRegisterSoftDelete() {
        Long taskId = 77L;

        assertFalse(taskBranchManager.isSoftDeleted(taskId));
        taskBranchManager.deleteBranch(TENANT_ID, taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));
    }

    @Test
    void deleteBranch_shouldThrowWhenTenantIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.deleteBranch(null, 99L));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void deleteBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.deleteBranch(TENANT_ID, null));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void hardDeleteBranch_shouldCallGitServiceWithForce() {
        Long taskId = 55L;

        taskBranchManager.hardDeleteBranch(TENANT_ID, taskId);

        verify(gitIntegrationService).deleteBranch(TENANT_ID, taskId, null, "task/55", true);
    }

    @Test
    void hardDeleteBranch_shouldRemoveFromSoftDeleteRegistry() {
        Long taskId = 55L;

        taskBranchManager.deleteBranch(TENANT_ID, taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        taskBranchManager.hardDeleteBranch(TENANT_ID, taskId);
        assertFalse(taskBranchManager.isSoftDeleted(taskId));
    }

    @Test
    void hardDeleteBranch_shouldThrowWhenTenantIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.hardDeleteBranch(null, 55L));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verifyNoInteractions(gitIntegrationService);
    }

    @Test
    void hardDeleteBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.hardDeleteBranch(TENANT_ID, null));
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

        taskBranchManager.deleteBranch(TENANT_ID, taskId);
        assertTrue(taskBranchManager.isSoftDeleted(taskId));

        taskBranchManager.recoverBranch(TENANT_ID, taskId, baseBranch);

        assertFalse(taskBranchManager.isSoftDeleted(taskId));
        verify(gitIntegrationService).createBranch(TENANT_ID, taskId, null, "task/88", baseBranch);
    }

    @Test
    void recoverBranch_shouldThrowWhenNotSoftDeleted() {
        Long taskId = 66L;

        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.recoverBranch(TENANT_ID, taskId, "main"));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void recoverBranch_shouldThrowWhenTenantIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.recoverBranch(null, 88L, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void recoverBranch_shouldThrowWhenTaskIdIsNull() {
        BaseException ex = assertThrows(BaseException.class,
                () -> taskBranchManager.recoverBranch(TENANT_ID, null, "main"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void purgeExpiredRecords_shouldRemoveExpiredEntries() {
        Long taskId = 111L;

        taskBranchManager.deleteBranch(TENANT_ID, taskId);
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
