package com.schemaplexai.task.domain;

import java.util.Set;

/**
 * Value vocabulary of the task board, fixed by the frontend contract
 * ({@code schemaplexai-ui/src/types/index.ts}, {@code SfTask}).
 */
public final class TaskBoardValues {

    private TaskBoardValues() {}

    /** Board columns, in lifecycle order. */
    public static final Set<String> STATUSES = Set.of(
            "BACKLOG", "QUEUED", "IN_PROGRESS", "AWAITING_REVIEW", "REVISING", "BLOCKED", "DONE");

    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_BACKLOG = "BACKLOG";

    /** P0 (highest) .. P3. */
    public static final Set<String> PRIORITIES = Set.of("P0", "P1", "P2", "P3");
    public static final String DEFAULT_PRIORITY = "P2";

    public static final Set<String> ASSIGNMENT_TYPES = Set.of("MANUAL", "AUTO", "MIXED");
    public static final String DEFAULT_ASSIGNMENT_TYPE = "MANUAL";

    /**
     * Presentation value for {@code JobRecord.maxRetries}: {@code sf_message_fail_log}
     * carries no max-retry column, so the board shows this platform default.
     */
    public static final int JOB_DEFAULT_MAX_RETRIES = 3;
}
