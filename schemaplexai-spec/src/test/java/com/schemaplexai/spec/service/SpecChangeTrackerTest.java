package com.schemaplexai.spec.service;

import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecChange;
import com.schemaplexai.spec.mapper.SfSpecChangeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecChangeTrackerTest {

    @Mock
    private SfSpecChangeMapper changeMapper;

    @InjectMocks
    private SpecChangeTracker tracker;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // recordCreation
    // ------------------------------------------------------------------

    @Test
    void recordCreation_writesAddRowPerInitializedField() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("Spec title");
        spec.setType("requirement");
        spec.setStatus("draft");
        spec.setContent("body");

        tracker.recordCreation(spec);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper, times(4)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(change -> {
                    assertThat(change.getSpecId()).isEqualTo(1L);
                    assertThat(change.getChangeType()).isEqualTo(SpecChangeTracker.CHANGE_ADD);
                    assertThat(change.getOldValue()).isNull();
                    assertThat(change.getChangedAt()).isNotNull();
                })
                .extracting(SfSpecChange::getFieldName, SfSpecChange::getNewValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("title", "Spec title"),
                        org.assertj.core.groups.Tuple.tuple("type", "requirement"),
                        org.assertj.core.groups.Tuple.tuple("status", "draft"),
                        org.assertj.core.groups.Tuple.tuple("content", "body"));
    }

    @Test
    void recordCreation_skipsNullFields() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("Spec title");
        spec.setType("requirement");
        spec.setStatus("draft");
        // content stays null

        tracker.recordCreation(spec);

        verify(changeMapper, times(3)).insert(any());
    }

    // ------------------------------------------------------------------
    // recordUpdate
    // ------------------------------------------------------------------

    @Test
    void recordUpdate_writesModifyRowsOnlyForChangedFields() {
        SfSpec before = new SfSpec();
        before.setId(1L);
        before.setTitle("old title");
        before.setType("requirement");
        before.setStatus("draft");
        before.setContent("same content");

        SfSpec after = new SfSpec();
        after.setId(1L);
        after.setTitle("new title");
        after.setType("requirement");
        after.setStatus("published");
        after.setContent("same content");

        tracker.recordUpdate(before, after, 9L);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(change -> {
                    assertThat(change.getChangeType()).isEqualTo(SpecChangeTracker.CHANGE_MODIFY);
                    assertThat(change.getVersionId()).isEqualTo(9L);
                })
                .extracting(SfSpecChange::getFieldName, SfSpecChange::getOldValue, SfSpecChange::getNewValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("title", "old title", "new title"),
                        org.assertj.core.groups.Tuple.tuple("status", "draft", "published"));
    }

    @Test
    void recordUpdate_noChanges_writesNothing() {
        SfSpec before = new SfSpec();
        before.setId(1L);
        before.setTitle("title");
        SfSpec after = new SfSpec();
        after.setId(1L);
        after.setTitle("title");

        tracker.recordUpdate(before, after, null);

        verify(changeMapper, never()).insert(any());
    }

    @Test
    void snapshot_copiesAuditedFields() {
        SfSpec spec = new SfSpec();
        spec.setId(7L);
        spec.setTitle("t");
        spec.setType("requirement");
        spec.setStatus("draft");
        spec.setContent("c");

        SfSpec copy = SpecChangeTracker.snapshot(spec);

        assertThat(copy).isNotSameAs(spec);
        assertThat(copy.getId()).isEqualTo(7L);
        assertThat(copy.getTitle()).isEqualTo("t");
        assertThat(copy.getType()).isEqualTo("requirement");
        assertThat(copy.getStatus()).isEqualTo("draft");
        assertThat(copy.getContent()).isEqualTo("c");
    }

    // ------------------------------------------------------------------
    // recordDeletion
    // ------------------------------------------------------------------

    @Test
    void recordDeletion_writesWholeDocumentDeleteRow() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("Deleted spec");

        tracker.recordDeletion(spec);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper).insert(captor.capture());
        SfSpecChange change = captor.getValue();
        assertThat(change.getChangeType()).isEqualTo(SpecChangeTracker.CHANGE_DELETE);
        assertThat(change.getFieldName()).isEqualTo(SpecChangeTracker.FIELD_DOCUMENT);
        assertThat(change.getOldValue()).isEqualTo("Deleted spec");
        assertThat(change.getNewValue()).isNull();
    }

    // ------------------------------------------------------------------
    // changedBy resolution
    // ------------------------------------------------------------------

    @Test
    void changedBy_resolvedFromSecurityContextPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("42", "N/A"));
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("t");

        tracker.recordDeletion(spec);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isEqualTo(42L);
    }

    @Test
    void changedBy_nullOutsideAuthenticatedRequest() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("t");

        tracker.recordDeletion(spec);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNull();
    }

    @Test
    void changedBy_nonNumericPrincipal_yieldsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("anonymousUser", "N/A"));
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setTitle("t");

        tracker.recordDeletion(spec);

        ArgumentCaptor<SfSpecChange> captor = ArgumentCaptor.forClass(SfSpecChange.class);
        verify(changeMapper).insert(captor.capture());
        assertThat(captor.getValue().getChangedBy()).isNull();
    }

    // ------------------------------------------------------------------
    // listBySpec
    // ------------------------------------------------------------------

    @Test
    void listBySpec_delegatesToMapper() {
        SfSpecChange change = new SfSpecChange();
        change.setSpecId(1L);
        when(changeMapper.selectList(any())).thenReturn(List.of(change));

        List<SfSpecChange> result = tracker.listBySpec(1L);

        assertThat(result).hasSize(1);
        verify(changeMapper).selectList(any());
    }
}
