package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/artifacts")
@RequiredArgsConstructor
@Tag(name = "制品管理", description = "制品上传、下载、验证与归档接口")
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping
    @Operation(summary = "创建制品")
    public Result<Long> create(@RequestBody SfArtifact artifact) {
        artifactService.save(artifact);
        return Result.success(artifact.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新制品")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfArtifact artifact) {
        artifact.setId(id);
        return Result.success(artifactService.updateById(artifact));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除制品")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(artifactService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取制品")
    public Result<SfArtifact> get(@PathVariable Long id) {
        SfArtifact artifact = artifactService.getById(id);
        if (artifact == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(artifact);
    }

    @GetMapping
    @Operation(summary = "列出所有制品")
    public Result<List<SfArtifact>> list() {
        return Result.success(artifactService.list());
    }

    @PostMapping("/upload")
    @Operation(summary = "上传制品")
    public Result<SfArtifact> uploadArtifact(@RequestBody SfArtifact artifact) {
        return Result.success(artifactService.uploadArtifact(artifact));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载制品")
    public Result<SfArtifact> downloadArtifact(@PathVariable Long id) {
        return Result.success(artifactService.downloadArtifact(id));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "验证制品")
    public Result<Boolean> validateArtifact(@PathVariable Long id) {
        return Result.success(artifactService.validateArtifact(id));
    }

    @GetMapping("/by-type")
    @Operation(summary = "根据类型列出制品")
    public Result<List<SfArtifact>> listArtifactsByType(@RequestParam String artifactType) {
        return Result.success(artifactService.listArtifactsByType(artifactType));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档制品")
    public Result<SfArtifact> archiveArtifact(@PathVariable Long id) {
        return Result.success(artifactService.archiveArtifact(id));
    }
}
