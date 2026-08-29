package com.schemaplexai.spec.domain;

/**
 * Spec lifecycle status vocabulary (sf_spec.status, VARCHAR(32)).
 * <p>
 * Unified lowercase vocabulary:
 * draft / in_review / approved / published / archived / rejected.
 * <p>
 * Only {@link #DRAFT} is editable (spec-management §3.1 "可编辑" column); every
 * other state rejects content edits until the document re-enters draft.
 * {@link #REJECTED} is a terminal state produced by the REJECTED review
 * decision (spec-management §4.1 "结束流程") and is deliberately distinct
 * from both {@link #DRAFT} (changes requested) and {@link #ARCHIVED}.
 */
public final class SpecStatus {

    public static final String DRAFT = "draft";
    public static final String IN_REVIEW = "in_review";
    public static final String APPROVED = "approved";
    public static final String PUBLISHED = "published";
    public static final String ARCHIVED = "archived";
    /** Terminal state: the review flow ended in rejection (§4.1 结束流程). */
    public static final String REJECTED = "rejected";

    private SpecStatus() {
    }

    /**
     * Only drafts may be edited. Any other lifecycle state (in_review,
     * approved, published, archived, rejected) rejects updates.
     */
    public static boolean isEditable(String status) {
        return DRAFT.equals(status);
    }

    /**
     * A review decision may only be applied to a spec that is still in the
     * active review path (draft or in_review). Terminal states — published,
     * archived, rejected, approved — reject further review submissions so a
     * REJECTED flow genuinely ends instead of being silently re-opened.
     */
    public static boolean isReviewable(String status) {
        return DRAFT.equals(status) || IN_REVIEW.equals(status);
    }
}
