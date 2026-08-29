package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecChange;
import com.schemaplexai.spec.mapper.SfSpecChangeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Field-level change audit for specs (spec-management §4.3 sf_spec_change,
 * REQ-12). Every spec mutation flows through one of the {@code record*}
 * methods so the audit trail — who changed which field from what to what —
 * cannot be bypassed by a service method writing sf_spec directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecChangeTracker {

    public static final String CHANGE_ADD = "ADD";
    public static final String CHANGE_MODIFY = "MODIFY";
    public static final String CHANGE_DELETE = "DELETE";

    /** Whole-document marker used for creation/deletion events. */
    public static final String FIELD_DOCUMENT = "*";

    private final SfSpecChangeMapper changeMapper;

    /**
     * Snapshot the audited fields (title/type/status/content) of a spec before
     * it is mutated, so {@link #recordUpdate} can diff old vs new values.
     */
    public static SfSpec snapshot(SfSpec spec) {
        SfSpec copy = new SfSpec();
        copy.setId(spec.getId());
        copy.setTitle(spec.getTitle());
        copy.setType(spec.getType());
        copy.setStatus(spec.getStatus());
        copy.setContent(spec.getContent());
        return copy;
    }

    /** Record the creation of a spec: one ADD row per initialized field. */
    public void recordCreation(SfSpec created) {
        Long specId = created.getId();
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        addField(specId, null, "title", created.getTitle(), userId, now);
        addField(specId, null, "type", created.getType(), userId, now);
        addField(specId, null, "status", created.getStatus(), userId, now);
        addField(specId, null, "content", created.getContent(), userId, now);
    }

    /**
     * Record the field-level difference between two states of the same spec.
     * Only fields whose value actually changed produce a MODIFY row.
     *
     * @param before    immutable snapshot taken before the mutation
     * @param after     the spec state after the mutation
     * @param versionId associated version snapshot id, if the change produced one
     */
    public void recordUpdate(SfSpec before, SfSpec after, Long versionId) {
        Long specId = after.getId();
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        diffField(specId, versionId, "title", before.getTitle(), after.getTitle(), userId, now);
        diffField(specId, versionId, "type", before.getType(), after.getType(), userId, now);
        diffField(specId, versionId, "status", before.getStatus(), after.getStatus(), userId, now);
        diffField(specId, versionId, "content", before.getContent(), after.getContent(), userId, now);
    }

    /** Record the deletion of a spec as a single whole-document DELETE row. */
    public void recordDeletion(SfSpec deleted) {
        insert(deleted.getId(), null, CHANGE_DELETE, FIELD_DOCUMENT, deleted.getTitle(), null,
                currentUserId(), LocalDateTime.now());
    }

    /** Audit trail of a single spec, oldest change first. */
    public List<SfSpecChange> listBySpec(Long specId) {
        LambdaQueryWrapper<SfSpecChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecChange::getSpecId, specId);
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            wrapper.eq(SfSpecChange::getTenantId, tenantId);
        }
        wrapper.orderByAsc(SfSpecChange::getChangedAt).orderByAsc(SfSpecChange::getId);
        return changeMapper.selectList(wrapper);
    }

    private void addField(Long specId, Long versionId, String fieldName, String newValue,
                          Long userId, LocalDateTime at) {
        if (newValue == null) {
            return;
        }
        insert(specId, versionId, CHANGE_ADD, fieldName, null, newValue, userId, at);
    }

    private void diffField(Long specId, Long versionId, String fieldName,
                           String oldValue, String newValue, Long userId, LocalDateTime at) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        insert(specId, versionId, CHANGE_MODIFY, fieldName, oldValue, newValue, userId, at);
    }

    private void insert(Long specId, Long versionId, String changeType, String fieldName,
                        String oldValue, String newValue, Long changedBy, LocalDateTime changedAt) {
        SfSpecChange change = new SfSpecChange();
        change.setSpecId(specId);
        change.setVersionId(versionId);
        change.setChangeType(changeType);
        change.setFieldName(fieldName);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setChangedBy(changedBy);
        change.setChangedAt(changedAt);
        changeMapper.insert(change);
        log.debug("Recorded {} change on spec {}.{}", changeType, specId, fieldName);
    }

    /**
     * Resolve the acting user from the SecurityContext. GatewayAuthFilter lifts
     * the JWT subject (userId) into the principal; outside an authenticated
     * request (unit tests, internal jobs) this yields null.
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
