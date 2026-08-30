package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Field-level spec change audit record (sf_spec_change, spec-management §4.3).
 * Every mutation of a spec — creation, content/status edit, publish, archive,
 * rollback, review decision, deletion — leaves a row recording who changed
 * which field from what to what, so governance documents stay auditable
 * (REQ-12). Rows are written through {@code SpecChangeTracker} at the service
 * write points; no service mutates sf_spec without an audit trail.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec_change")
public class SfSpecChange extends BaseEntity {

    private Long specId;

    /** Version snapshot associated with the change, when one exists. */
    private Long versionId;

    /** ADD / MODIFY / DELETE. */
    private String changeType;

    /** Changed field: title / type / status / content, or "*" for whole-document events. */
    private String fieldName;

    private String oldValue;

    private String newValue;

    private Long changedBy;

    private LocalDateTime changedAt;
}
