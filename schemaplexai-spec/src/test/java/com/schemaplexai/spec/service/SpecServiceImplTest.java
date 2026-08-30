package com.schemaplexai.spec.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecTemplateMapper;
import com.schemaplexai.spec.mapper.SfSpecVersionMapper;
import com.schemaplexai.spec.service.SpecChangeTracker;
import com.schemaplexai.spec.service.impl.SpecServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecServiceImplTest {

    @Mock
    private SfSpecMapper specMapper;

    @Mock
    private SfSpecVersionMapper specVersionMapper;

    @Mock
    private SfSpecTemplateMapper specTemplateMapper;

    @Mock
    private SpecChangeTracker changeTracker;

    @InjectMocks
    private SpecServiceImpl specService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specService, "baseMapper", specMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // createSpec
    // ------------------------------------------------------------------

    @Test
    void createSpec_nullSpec_throwsParamError() {
        assertThatThrownBy(() -> specService.createSpec(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).insert(any());
    }

    @Test
    void createSpec_clientSuppliedStatus_isForcedToDraft() {
        SfSpec input = new SfSpec();
        input.setTitle("T");
        input.setStatus("published");

        SfSpec result = specService.createSpec(input);

        assertThat(result.getStatus()).isEqualTo("draft");
        verify(specMapper).insert(input);
        verify(changeTracker).recordCreation(input);
    }

    // ------------------------------------------------------------------
    // updateSpec (draft-only edit guard)
    // ------------------------------------------------------------------

    @Test
    void updateSpec_nullId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.updateSpec(null, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void updateSpec_nullBody_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.updateSpec(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void updateSpec_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.updateSpec(1L, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void updateSpec_draft_mergesNonNullFields() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("draft");
        existing.setTitle("old title");
        existing.setContent("old content");
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.updateById(existing)).thenReturn(1);

        SfSpec update = new SfSpec();
        update.setTitle("new title");

        boolean result = specService.updateSpec(1L, update);

        assertThat(result).isTrue();
        assertThat(existing.getTitle()).isEqualTo("new title");
        assertThat(existing.getContent()).isEqualTo("old content");
        assertThat(existing.getUpdatedAt()).isNotNull();
        verify(specMapper).updateById(existing);
        verify(changeTracker).recordUpdate(any(SfSpec.class), eq(existing), isNull());
    }

    @Test
    void updateSpec_draft_cannotRewriteLifecycleStatus() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("draft");
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.updateById(existing)).thenReturn(1);

        SfSpec update = new SfSpec();
        update.setStatus("published");

        specService.updateSpec(1L, update);

        assertThat(existing.getStatus()).isEqualTo("draft");
    }

    @Test
    void updateSpec_published_throwsForbidden() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("published");
        when(specMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> specService.updateSpec(1L, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void updateSpec_approved_throwsForbidden() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("approved");
        when(specMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> specService.updateSpec(1L, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void updateSpec_archived_throwsForbidden() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("archived");
        when(specMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> specService.updateSpec(1L, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void updateSpec_staleClientVersion_throwsConflict() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("draft");
        existing.setVersion(5);
        when(specMapper.selectById(1L)).thenReturn(existing);

        SfSpec update = new SfSpec();
        update.setTitle("new title");
        update.setVersion(3); // client edited from an older revision

        assertThatThrownBy(() -> specService.updateSpec(1L, update))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void updateSpec_matchingClientVersion_proceeds() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("draft");
        existing.setVersion(5);
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.updateById(existing)).thenReturn(1);

        SfSpec update = new SfSpec();
        update.setTitle("new title");
        update.setVersion(5);

        assertThat(specService.updateSpec(1L, update)).isTrue();
        verify(specMapper).updateById(existing);
    }

    @Test
    void updateSpec_zeroRowGuardedUpdate_throwsConflict() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setStatus("draft");
        existing.setVersion(5);
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.updateById(existing)).thenReturn(0); // a concurrent writer won the race

        assertThatThrownBy(() -> specService.updateSpec(1L, new SfSpec()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());
    }

    // ------------------------------------------------------------------
    // publishSpec
    // ------------------------------------------------------------------

    @Test
    void publishSpec_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.publishSpec(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void publishSpec_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.publishSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void publishSpec_noExistingVersion_createsVersionOne() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("spec content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.APPROVED);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);
        when(specVersionMapper.selectOne(any())).thenReturn(null);

        SfSpecVersion result = specService.publishSpec(1L);

        assertThat(result.getVersion()).isEqualTo("1");
        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("spec content");
        verify(specMapper).updateById(spec);
        verify(specVersionMapper).insert(any(SfSpecVersion.class));
    }

    @Test
    void publishSpec_withExistingVersion_incrementsVersion() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("spec content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.APPROVED);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecVersion latest = new SfSpecVersion();
        latest.setVersion("5");
        when(specVersionMapper.selectOne(any())).thenReturn(latest);

        SfSpecVersion result = specService.publishSpec(1L);

        assertThat(result.getVersion()).isEqualTo("6");
    }

    @Test
    void publishSpec_withNonNumericVersion_defaultsToOne() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("spec content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.APPROVED);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecVersion latest = new SfSpecVersion();
        latest.setVersion("v1.0");
        when(specVersionMapper.selectOne(any())).thenReturn(latest);

        SfSpecVersion result = specService.publishSpec(1L);

        assertThat(result.getVersion()).isEqualTo("1");
    }

    @Test
    void publishSpec_setsStatusToPublished() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.APPROVED);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);
        when(specVersionMapper.selectOne(any())).thenReturn(null);

        specService.publishSpec(1L);

        assertThat(spec.getStatus()).isEqualTo("published");
        assertThat(spec.getUpdatedAt()).isNotNull();
    }

    @Test
    void publishSpec_concurrentModification_throwsConflict() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.APPROVED);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(0);

        assertThatThrownBy(() -> specService.publishSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(specVersionMapper, never()).insert(any());
    }

    @Test
    void publishSpec_fromDraft_throwsForbidden() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setContent("content");
        spec.setStatus(com.schemaplexai.spec.domain.SpecStatus.DRAFT);
        when(specMapper.selectById(1L)).thenReturn(spec);

        // Review NEW-06: publishing must require an approved review decision.
        assertThatThrownBy(() -> specService.publishSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(specMapper, never()).updateById(any());
        verify(specVersionMapper, never()).insert(any());
    }

    // ------------------------------------------------------------------
    // archiveSpec
    // ------------------------------------------------------------------

    @Test
    void archiveSpec_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.archiveSpec(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void archiveSpec_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.archiveSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void archiveSpec_success_returnsTrue() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);

        boolean result = specService.archiveSpec(1L);

        assertThat(result).isTrue();
        assertThat(spec.getStatus()).isEqualTo("archived");
        assertThat(spec.getUpdatedAt()).isNotNull();
    }

    @Test
    void archiveSpec_concurrentModification_throwsConflict() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(0);

        assertThatThrownBy(() -> specService.archiveSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());
    }

    // ------------------------------------------------------------------
    // rollbackSpec
    // ------------------------------------------------------------------

    @Test
    void rollbackSpec_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.rollbackSpec(null, 5L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void rollbackSpec_nullVersionId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.rollbackSpec(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
    }

    @Test
    void rollbackSpec_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.rollbackSpec(1L, 5L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void rollbackSpec_versionNotFound_throwsSpecNotFound() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specVersionMapper.selectById(5L)).thenReturn(null);

        assertThatThrownBy(() -> specService.rollbackSpec(1L, 5L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void rollbackSpec_snapshotOfDifferentSpec_throwsParamError() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(spec);

        SfSpecVersion foreign = new SfSpecVersion();
        foreign.setId(5L);
        foreign.setSpecId(2L);
        when(specVersionMapper.selectById(5L)).thenReturn(foreign);

        assertThatThrownBy(() -> specService.rollbackSpec(1L, 5L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).updateById(any());
    }

    @Test
    void rollbackSpec_success_restoresSnapshotContentAsDraft() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("published");
        spec.setContent("bad published content");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(1);

        SfSpecVersion snapshot = new SfSpecVersion();
        snapshot.setId(5L);
        snapshot.setSpecId(1L);
        snapshot.setContent("known good content");
        when(specVersionMapper.selectById(5L)).thenReturn(snapshot);

        SfSpec result = specService.rollbackSpec(1L, 5L);

        assertThat(result.getContent()).isEqualTo("known good content");
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(specMapper).updateById(spec);
        // The historical snapshot itself must never be rewritten.
        verify(specVersionMapper, never()).updateById(any());
    }

    @Test
    void rollbackSpec_concurrentModification_throwsConflict() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("published");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specMapper.updateById(spec)).thenReturn(0);

        SfSpecVersion snapshot = new SfSpecVersion();
        snapshot.setId(5L);
        snapshot.setSpecId(1L);
        snapshot.setContent("known good content");
        when(specVersionMapper.selectById(5L)).thenReturn(snapshot);

        assertThatThrownBy(() -> specService.rollbackSpec(1L, 5L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());
    }

    // ------------------------------------------------------------------
    // deleteSpec (tracked deletion)
    // ------------------------------------------------------------------

    @Test
    void deleteSpec_nullId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.deleteSpec(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specMapper, never()).selectById(any());
        verify(specMapper, never()).deleteById(any());
    }

    @Test
    void deleteSpec_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.deleteSpec(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());

        verify(specMapper, never()).deleteById(any());
        verify(changeTracker, never()).recordDeletion(any());
    }

    @Test
    void deleteSpec_success_recordsDeleteTrail() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        existing.setTitle("Doomed spec");
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.deleteById(1L)).thenReturn(1);

        boolean result = specService.deleteSpec(1L);

        assertThat(result).isTrue();
        verify(specMapper).deleteById(1L);
        verify(changeTracker).recordDeletion(existing);
    }

    @Test
    void deleteSpec_zeroRows_noAuditRow() {
        SfSpec existing = new SfSpec();
        existing.setId(1L);
        when(specMapper.selectById(1L)).thenReturn(existing);
        when(specMapper.deleteById(1L)).thenReturn(0);

        boolean result = specService.deleteSpec(1L);

        assertThat(result).isFalse();
        verify(changeTracker, never()).recordDeletion(any());
    }

    // ------------------------------------------------------------------
    // getLatestVersion
    // ------------------------------------------------------------------

    @Test
    void getLatestVersion_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.getLatestVersion(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specVersionMapper, never()).selectOne(any());
    }

    @Test
    void getLatestVersion_noVersion_returnsEmpty() {
        when(specVersionMapper.selectOne(any())).thenReturn(null);

        Optional<SfSpecVersion> result = specService.getLatestVersion(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getLatestVersion_found_returnsVersion() {
        SfSpecVersion version = new SfSpecVersion();
        version.setVersion("3");
        version.setSpecId(1L);
        when(specVersionMapper.selectOne(any())).thenReturn(version);

        Optional<SfSpecVersion> result = specService.getLatestVersion(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo("3");
    }

    @Test
    void getLatestVersion_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        SfSpecVersion version = new SfSpecVersion();
        version.setVersion("2");
        when(specVersionMapper.selectOne(any())).thenReturn(version);

        Optional<SfSpecVersion> result = specService.getLatestVersion(1L);

        assertThat(result).isPresent();
    }

    // ------------------------------------------------------------------
    // compareVersions
    // ------------------------------------------------------------------

    @Test
    void compareVersions_nullSpecId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.compareVersions(null, "1", "2"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specVersionMapper, never()).selectList(any());
    }

    @Test
    void compareVersions_blankVersionA_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.compareVersions(1L, " ", "2"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specVersionMapper, never()).selectList(any());
    }

    @Test
    void compareVersions_blankVersionB_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.compareVersions(1L, "1", " "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specVersionMapper, never()).selectList(any());
    }

    @Test
    void compareVersions_returnsMatchingVersions() {
        SfSpecVersion v1 = new SfSpecVersion();
        v1.setVersion("1");
        SfSpecVersion v2 = new SfSpecVersion();
        v2.setVersion("2");
        when(specVersionMapper.selectList(any())).thenReturn(List.of(v1, v2));

        List<SfSpecVersion> result = specService.compareVersions(1L, "1", "2");

        assertThat(result).hasSize(2);
    }

    @Test
    void compareVersions_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(specVersionMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfSpecVersion> result = specService.compareVersions(1L, "1", "2");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // createFromTemplate
    // ------------------------------------------------------------------

    @Test
    void createFromTemplate_nullTemplateId_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.createFromTemplate(null, "Title", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specTemplateMapper, never()).selectById(any());
        verify(specMapper, never()).insert(any());
    }

    @Test
    void createFromTemplate_blankTitle_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.createFromTemplate(1L, " ", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specTemplateMapper, never()).selectById(any());
        verify(specMapper, never()).insert(any());
    }

    @Test
    void createFromTemplate_blankType_throwsParamErrorWithoutLookup() {
        assertThatThrownBy(() -> specService.createFromTemplate(1L, "Title", " "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(specTemplateMapper, never()).selectById(any());
        verify(specMapper, never()).insert(any());
    }

    @Test
    void createFromTemplate_templateNotFound_throwsNotFound() {
        when(specTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specService.createFromTemplate(1L, "Title", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createFromTemplate_success_createsSpecFromTemplate() {
        SfSpecTemplate template = new SfSpecTemplate();
        template.setId(1L);
        template.setContent("template content");
        when(specTemplateMapper.selectById(1L)).thenReturn(template);

        SfSpec result = specService.createFromTemplate(1L, "My Spec", "requirement");

        assertThat(result.getTitle()).isEqualTo("My Spec");
        assertThat(result.getType()).isEqualTo("requirement");
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getContent()).isEqualTo("template content");
        verify(specMapper).insert(any(SfSpec.class));
        verify(changeTracker).recordCreation(result);
    }
}
