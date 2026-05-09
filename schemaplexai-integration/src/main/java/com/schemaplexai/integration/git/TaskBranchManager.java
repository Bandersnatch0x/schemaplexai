package com.schemaplexai.integration.git;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.GitIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages task-scoped Git branches with 30-day soft-delete semantics.
 *
 * <p>Branch naming convention: {@code task/<taskId>}.
 * <p>Deleted branches are tracked in a soft-delete registry for 30 days before
 * hard deletion. This allows recovery and audit trails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBranchManager {

    private static final String BRANCH_PREFIX = "task/";
    private static final int SOFT_DELETE_DAYS = 30;

    private final GitIntegrationService gitIntegrationService;

    /** Soft-delete registry: branch name -> deletion metadata. */
    private final Map<String, SoftDeleteRecord> softDeleteRegistry = new ConcurrentHashMap<>();

    /**
     * Create a new task branch from the given base branch.
     *
     * @param taskId     the task identifier (used in branch name)
     * @param baseBranch the base branch to fork from (e.g. "main")
     * @throws BaseException if taskId is null or branch creation fails
     */
    public void createBranch(Long taskId, String baseBranch) {
        if (taskId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "taskId must not be null");
        }
        String branchName = BRANCH_PREFIX + taskId;

        // If previously soft-deleted, remove from registry (reactivation)
        SoftDeleteRecord previous = softDeleteRegistry.remove(branchName);
        if (previous != null) {
            log.info("Reactivating previously soft-deleted branch: {}", branchName);
        }

        gitIntegrationService.createBranch(taskId, null, branchName, baseBranch);
        log.info("Task branch created: {} from base: {}", branchName, baseBranch);
    }

    /**
     * Soft-delete a task branch. The branch is removed from the remote but kept
     * in a registry for {@value #SOFT_DELETE_DAYS} days for potential recovery.
     *
     * @param taskId the task identifier
     * @throws BaseException if taskId is null or deletion fails
     */
    public void deleteBranch(Long taskId) {
        if (taskId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "taskId must not be null");
        }
        String branchName = BRANCH_PREFIX + taskId;

        gitIntegrationService.deleteBranch(taskId, null, branchName, false);

        Instant deletedAt = Instant.now();
        Instant expiresAt = deletedAt.plus(SOFT_DELETE_DAYS, ChronoUnit.DAYS);
        softDeleteRegistry.put(branchName, new SoftDeleteRecord(taskId, branchName, deletedAt, expiresAt));

        log.info("Task branch soft-deleted: {} (expires at {})", branchName, expiresAt);
    }

    /**
     * Hard-delete a task branch, bypassing the soft-delete registry.
     * Use with caution — no recovery possible.
     *
     * @param taskId the task identifier
     * @throws BaseException if taskId is null or deletion fails
     */
    public void hardDeleteBranch(Long taskId) {
        if (taskId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "taskId must not be null");
        }
        String branchName = BRANCH_PREFIX + taskId;

        gitIntegrationService.deleteBranch(taskId, null, branchName, true);
        softDeleteRegistry.remove(branchName);

        log.info("Task branch hard-deleted: {}", branchName);
    }

    /**
     * Check whether a branch is in the soft-delete registry.
     *
     * @param taskId the task identifier
     * @return true if the branch was soft-deleted and not yet expired
     */
    public boolean isSoftDeleted(Long taskId) {
        if (taskId == null) {
            return false;
        }
        String branchName = BRANCH_PREFIX + taskId;
        SoftDeleteRecord record = softDeleteRegistry.get(branchName);
        if (record == null) {
            return false;
        }
        if (Instant.now().isAfter(record.expiresAt)) {
            softDeleteRegistry.remove(branchName);
            return false;
        }
        return true;
    }

    /**
     * Recover a soft-deleted branch by recreating it from the default base.
     *
     * @param taskId     the task identifier
     * @param baseBranch the base branch to recreate from
     * @throws BaseException if the branch is not soft-deleted or recovery fails
     */
    public void recoverBranch(Long taskId, String baseBranch) {
        if (taskId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "taskId must not be null");
        }
        String branchName = BRANCH_PREFIX + taskId;
        SoftDeleteRecord record = softDeleteRegistry.get(branchName);
        if (record == null || Instant.now().isAfter(record.expiresAt)) {
            throw new BaseException(ResultCode.NOT_FOUND,
                    "Branch " + branchName + " is not available for recovery");
        }

        softDeleteRegistry.remove(branchName);
        gitIntegrationService.createBranch(taskId, null, branchName, baseBranch);
        log.info("Task branch recovered: {} from base: {}", branchName, baseBranch);
    }

    /** Purge all expired soft-delete records. Called periodically by a scheduled job. */
    public int purgeExpiredRecords() {
        Instant now = Instant.now();
        var expired = softDeleteRegistry.entrySet().stream()
                .filter(e -> now.isAfter(e.getValue().expiresAt))
                .map(Map.Entry::getKey)
                .toList();
        expired.forEach(softDeleteRegistry::remove);
        if (!expired.isEmpty()) {
            log.info("Purged {} expired soft-delete records", expired.size());
        }
        return expired.size();
    }

    // --- Internal record ---

    private record SoftDeleteRecord(Long taskId, String branchName,
                                     Instant deletedAt, Instant expiresAt) {
    }
}
