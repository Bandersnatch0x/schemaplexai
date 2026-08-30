package com.schemaplexai.task.web;

/**
 * Request-scoped holder for the authenticated user id lifted from the
 * gateway-injected {@code X-User-Id} header (see {@link TenantContextFilter}).
 * Used as the comment author on {@code POST /task/tasks/{id}/comments}.
 */
public final class TaskUserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private TaskUserContextHolder() {}

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /** @return the current user id, or {@code null} when absent/unparsable */
    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
