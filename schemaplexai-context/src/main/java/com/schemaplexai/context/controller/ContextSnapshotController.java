package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.service.ContextSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/snapshots")
@RequiredArgsConstructor
@Tag(name = "上下文快照管理", description = "上下文快照创建、恢复与对比接口")
public class ContextSnapshotController {

    private final ContextSnapshotService contextSnapshotService;

    @PostMapping
    @Operation(summary = "创建上下文快照")
    public Result<Long> create(@RequestBody SfContextSnapshot snapshot) {
        contextSnapshotService.save(snapshot);
        return Result.success(snapshot.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新上下文快照")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfContextSnapshot snapshot) {
        snapshot.setId(id);
        return Result.success(contextSnapshotService.updateById(snapshot));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除上下文快照")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(contextSnapshotService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取上下文快照")
    public Result<SfContextSnapshot> get(@PathVariable Long id) {
        SfContextSnapshot snapshot = contextSnapshotService.getById(id);
        if (snapshot == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(snapshot);
    }

    @PostMapping("/create")
    @Operation(summary = "创建快照")
    public Result<SfContextSnapshot> createSnapshot(@RequestParam Long contextId,
                                                    @RequestParam String snapshotJson) {
        return Result.success(contextSnapshotService.createSnapshot(contextId, snapshotJson));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "从快照恢复")
    public Result<String> restoreFromSnapshot(@PathVariable Long id) {
        return Result.success(contextSnapshotService.restoreFromSnapshot(id));
    }

    @GetMapping("/by-context")
    @Operation(summary = "根据上下文ID列出快照")
    public Result<List<SfContextSnapshot>> listSnapshotsByContext(@RequestParam Long contextId) {
        return Result.success(contextSnapshotService.listSnapshotsByContext(contextId));
    }

    @GetMapping("/compare")
    @Operation(summary = "对比两个快照")
    public Result<String> compareSnapshots(@RequestParam Long snapshotIdA,
                                           @RequestParam Long snapshotIdB) {
        return Result.success(contextSnapshotService.compareSnapshots(snapshotIdA, snapshotIdB));
    }
}
