package com.schemaplexai.context.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.context.entity.SfContextSnapshot;

import java.util.List;

public interface ContextSnapshotService extends IService<SfContextSnapshot> {

    /**
     * Create a snapshot for a given context.
     */
    SfContextSnapshot createSnapshot(Long contextId, String snapshotJson);

    /**
     * Restore context state from a snapshot.
     */
    String restoreFromSnapshot(Long snapshotId);

    /**
     * List all snapshots for a context, ordered by version desc.
     */
    List<SfContextSnapshot> listSnapshotsByContext(Long contextId);

    /**
     * Compare two snapshots and return a simple diff description.
     */
    String compareSnapshots(Long snapshotIdA, Long snapshotIdB);
}
