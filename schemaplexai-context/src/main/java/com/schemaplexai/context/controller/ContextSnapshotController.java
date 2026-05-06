package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.service.ContextSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/context/snapshots")
@RequiredArgsConstructor
public class ContextSnapshotController {

    private final ContextSnapshotService contextSnapshotService;

    @PostMapping
    public Result<Long> create(@RequestBody SfContextSnapshot snapshot) {
        contextSnapshotService.save(snapshot);
        return Result.success(snapshot.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfContextSnapshot snapshot) {
        snapshot.setId(id);
        return Result.success(contextSnapshotService.updateById(snapshot));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(contextSnapshotService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfContextSnapshot> get(@PathVariable Long id) {
        SfContextSnapshot snapshot = contextSnapshotService.getById(id);
        if (snapshot == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(snapshot);
    }

    @PostMapping("/create")
    public Result<SfContextSnapshot> createSnapshot(@RequestParam Long contextId,
                                                    @RequestParam String snapshotJson) {
        return Result.success(contextSnapshotService.createSnapshot(contextId, snapshotJson));
    }

    @PostMapping("/{id}/restore")
    public Result<String> restoreFromSnapshot(@PathVariable Long id) {
        return Result.success(contextSnapshotService.restoreFromSnapshot(id));
    }

    @GetMapping("/by-context")
    public Result<List<SfContextSnapshot>> listSnapshotsByContext(@RequestParam Long contextId) {
        return Result.success(contextSnapshotService.listSnapshotsByContext(contextId));
    }

    @GetMapping("/compare")
    public Result<String> compareSnapshots(@RequestParam Long snapshotIdA,
                                           @RequestParam Long snapshotIdB) {
        return Result.success(contextSnapshotService.compareSnapshots(snapshotIdA, snapshotIdB));
    }
}
