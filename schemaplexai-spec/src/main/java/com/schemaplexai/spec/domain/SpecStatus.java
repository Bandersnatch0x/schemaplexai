package com.schemaplexai.spec.domain;

/**
 * Spec lifecycle status vocabulary (sf_spec.status, VARCHAR(32)).
 * <p>
 * Unified lowercase vocabulary: draft / in_review / approved / published / archived.
 * Only {@link #DRAFT} is editable (spec-management §3.1 "可编辑" column); every
 * other state rejects content edits until the document re-enters draft.
 */
public final class SpecStatus {

    public static final String DRAFT = "draft";
    public static final String IN_REVIEW = "in_review";
    public static final String APPROVED = "approved";
    public static final String PUBLISHED = "published";
    public static final String ARCHIVED = "archived";

    private SpecStatus() {
    }

    /**
     * Only drafts may be edited. Any other lifecycle state (in_review,
     * approved, published, archived) rejects updates.
     */
    public static boolean isEditable(String status) {
        return DRAFT.equals(status);
    }
}
