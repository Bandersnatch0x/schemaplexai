package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping
    public Result<Long> create(@RequestBody SfArtifact artifact) {
        artifactService.save(artifact);
        return Result.success(artifact.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfArtifact artifact) {
        artifact.setId(id);
        return Result.success(artifactService.updateById(artifact));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(artifactService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfArtifact> get(@PathVariable Long id) {
        SfArtifact artifact = artifactService.getById(id);
        if (artifact == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(artifact);
    }

    @GetMapping
    public Result<List<SfArtifact>> list() {
        return Result.success(artifactService.list());
    }

    @PostMapping("/upload")
    public Result<SfArtifact> uploadArtifact(@RequestBody SfArtifact artifact) {
        return Result.success(artifactService.uploadArtifact(artifact));
    }

    @GetMapping("/{id}/download")
    public Result<SfArtifact> downloadArtifact(@PathVariable Long id) {
        return Result.success(artifactService.downloadArtifact(id));
    }

    @PostMapping("/{id}/validate")
    public Result<Boolean> validateArtifact(@PathVariable Long id) {
        return Result.success(artifactService.validateArtifact(id));
    }

    @GetMapping("/by-type")
    public Result<List<SfArtifact>> listArtifactsByType(@RequestParam String artifactType) {
        return Result.success(artifactService.listArtifactsByType(artifactType));
    }

    @PostMapping("/{id}/archive")
    public Result<SfArtifact> archiveArtifact(@PathVariable Long id) {
        return Result.success(artifactService.archiveArtifact(id));
    }
}
