package com.schemaplexai.spec.security;

/**
 * Authority names of the Spec-domain permission matrix (spec-management §3.2).
 * <p>
 * The spec's conceptual roles map onto these authorities:
 * <ul>
 *   <li>编辑者 (editor)   -> {@link #WRITE}</li>
 *   <li>审批者 (approver) -> {@link #REVIEW}, {@link #PUBLISH}</li>
 *   <li>管理员 (admin)    -> all authorities incl. {@link #ROLLBACK}, {@link #DELETE}</li>
 *   <li>读者 (reader)     -> any authenticated principal (read endpoints carry
 *       no extra authority requirement)</li>
 * </ul>
 */
public final class SpecAuthorities {

    /** Create / edit specs and create new versions. */
    public static final String WRITE = "spec:write";

    /** Submit and manage review decisions. */
    public static final String REVIEW = "spec:review";

    /** Publish a spec version. */
    public static final String PUBLISH = "spec:publish";

    /** Roll back to a historical version. */
    public static final String ROLLBACK = "spec:rollback";

    /** Delete (and archive) specs and version snapshots. */
    public static final String DELETE = "spec:delete";

    private SpecAuthorities() {
    }
}
