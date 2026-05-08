package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.service.ArtifactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactControllerTest {

    @Mock
    private ArtifactService artifactService;

    @InjectMocks
    private ArtifactController artifactController;

    private SfArtifact artifact;

    @BeforeEach
    void setUp() {
        artifact = new SfArtifact();
        artifact.setId(1L);
        artifact.setName("test-artifact");
        artifact.setVersion("1.0.0");
        artifact.setArtifactType("jar");
        artifact.setStatus(0);
    }

    @Test
    void create_returnsId() {
        when(artifactService.save(any())).thenReturn(true);

        Result<Long> result = artifactController.create(artifact);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(artifactService.updateById(any())).thenReturn(true);

        Result<Boolean> result = artifactController.update(1L, artifact);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(artifactService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = artifactController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(artifactService.getById(1L)).thenReturn(artifact);

        Result<SfArtifact> result = artifactController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("test-artifact");
    }

    @Test
    void get_notFound() {
        when(artifactService.getById(1L)).thenReturn(null);

        Result<SfArtifact> result = artifactController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void list_returnsArtifacts() {
        when(artifactService.list()).thenReturn(List.of(artifact));

        Result<List<SfArtifact>> result = artifactController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void uploadArtifact_returnsArtifact() {
        when(artifactService.uploadArtifact(any())).thenReturn(artifact);

        Result<SfArtifact> result = artifactController.uploadArtifact(artifact);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("test-artifact");
    }

    @Test
    void downloadArtifact_returnsArtifact() {
        when(artifactService.downloadArtifact(1L)).thenReturn(artifact);

        Result<SfArtifact> result = artifactController.downloadArtifact(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void validateArtifact_returnsBoolean() {
        when(artifactService.validateArtifact(1L)).thenReturn(true);

        Result<Boolean> result = artifactController.validateArtifact(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void listArtifactsByType_returnsArtifacts() {
        when(artifactService.listArtifactsByType("jar")).thenReturn(List.of(artifact));

        Result<List<SfArtifact>> result = artifactController.listArtifactsByType("jar");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void archiveArtifact_returnsArtifact() {
        when(artifactService.archiveArtifact(1L)).thenReturn(artifact);

        Result<SfArtifact> result = artifactController.archiveArtifact(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }
}
