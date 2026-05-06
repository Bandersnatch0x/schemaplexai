package com.schemaplexai.ops.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.mapper.ArtifactMapper;
import com.schemaplexai.ops.service.ArtifactServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceImplTest {

    @Mock
    private ArtifactMapper artifactMapper;

    @InjectMocks
    private ArtifactServiceImpl artifactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(artifactService, "baseMapper", artifactMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // uploadArtifact
    // ------------------------------------------------------------------

    @Test
    void uploadArtifact_nullName_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setArtifactType("jar");
        artifact.setFileUrl("http://example.com/file.jar");

        assertThatThrownBy(() -> artifactService.uploadArtifact(artifact))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void uploadArtifact_blankName_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setName("   ");
        artifact.setArtifactType("jar");
        artifact.setFileUrl("http://example.com/file.jar");

        assertThatThrownBy(() -> artifactService.uploadArtifact(artifact))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void uploadArtifact_nullType_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setName("my-artifact");
        artifact.setFileUrl("http://example.com/file.jar");

        assertThatThrownBy(() -> artifactService.uploadArtifact(artifact))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void uploadArtifact_nullFileUrl_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setName("my-artifact");
        artifact.setArtifactType("jar");

        assertThatThrownBy(() -> artifactService.uploadArtifact(artifact))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void uploadArtifact_success_setsActiveStatus() {
        SfArtifact artifact = new SfArtifact();
        artifact.setName("my-artifact");
        artifact.setArtifactType("jar");
        artifact.setFileUrl("http://example.com/file.jar");

        SfArtifact result = artifactService.uploadArtifact(artifact);

        assertThat(result.getStatus()).isEqualTo(0);
        assertThat(result.getName()).isEqualTo("my-artifact");
        verify(artifactMapper).insert(artifact);
    }

    // ------------------------------------------------------------------
    // downloadArtifact
    // ------------------------------------------------------------------

    @Test
    void downloadArtifact_notFound_throwsNotFound() {
        when(artifactMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> artifactService.downloadArtifact(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void downloadArtifact_archived_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setStatus(1);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        assertThatThrownBy(() -> artifactService.downloadArtifact(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void downloadArtifact_active_returnsArtifact() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setName("my-artifact");
        artifact.setStatus(0);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfArtifact result = artifactService.downloadArtifact(1L);

        assertThat(result.getName()).isEqualTo("my-artifact");
    }

    @Test
    void downloadArtifact_nullStatus_returnsArtifact() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setName("my-artifact");
        artifact.setStatus(null);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfArtifact result = artifactService.downloadArtifact(1L);

        assertThat(result.getName()).isEqualTo("my-artifact");
    }

    // ------------------------------------------------------------------
    // validateArtifact
    // ------------------------------------------------------------------

    @Test
    void validateArtifact_notFound_throwsNotFound() {
        when(artifactMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> artifactService.validateArtifact(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void validateArtifact_missingFileUrl_returnsFalse() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setFileUrl(null);
        artifact.setVersion("1.0");
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        boolean result = artifactService.validateArtifact(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateArtifact_blankFileUrl_returnsFalse() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setFileUrl("");
        artifact.setVersion("1.0");
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        boolean result = artifactService.validateArtifact(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateArtifact_missingVersion_returnsFalse() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setFileUrl("http://example.com/file.jar");
        artifact.setVersion(null);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        boolean result = artifactService.validateArtifact(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateArtifact_valid_returnsTrue() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setName("my-artifact");
        artifact.setFileUrl("http://example.com/file.jar");
        artifact.setVersion("1.0");
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        boolean result = artifactService.validateArtifact(1L);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // listArtifactsByType
    // ------------------------------------------------------------------

    @Test
    void listArtifactsByType_nullType_throwsParamError() {
        assertThatThrownBy(() -> artifactService.listArtifactsByType(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listArtifactsByType_blankType_throwsParamError() {
        assertThatThrownBy(() -> artifactService.listArtifactsByType("   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listArtifactsByType_returnsArtifacts() {
        SfArtifact a1 = new SfArtifact();
        a1.setName("A1");
        when(artifactMapper.selectList(any())).thenReturn(List.of(a1));

        List<SfArtifact> result = artifactService.listArtifactsByType("jar");

        assertThat(result).hasSize(1);
    }

    @Test
    void listArtifactsByType_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(artifactMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfArtifact> result = artifactService.listArtifactsByType("jar");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // archiveArtifact
    // ------------------------------------------------------------------

    @Test
    void archiveArtifact_notFound_throwsNotFound() {
        when(artifactMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> artifactService.archiveArtifact(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void archiveArtifact_alreadyArchived_throwsParamError() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setStatus(1);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        assertThatThrownBy(() -> artifactService.archiveArtifact(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void archiveArtifact_success_setsStatusToArchived() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setName("my-artifact");
        artifact.setStatus(0);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfArtifact result = artifactService.archiveArtifact(1L);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(artifactMapper).updateById(artifact);
    }

    @Test
    void archiveArtifact_nullStatus_archivesSuccessfully() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setStatus(null);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfArtifact result = artifactService.archiveArtifact(1L);

        assertThat(result.getStatus()).isEqualTo(1);
    }
}
