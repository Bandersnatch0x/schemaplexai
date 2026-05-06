---
title: ContextSnapshotService
type: service
source: schemaplexai-context/src/main/java/com/schemaplexai/context/service/ContextSnapshotService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, context, snapshot, versioning, restore]
confidence: high
---

# ContextSnapshotService

> One-sentence summary: Interface for creating, restoring, listing, and comparing versioned snapshots of context state.

## Responsibilities

1. Create snapshots of a context's current state (serialized JSON)
2. Restore context state from a previously saved snapshot
3. List all snapshots for a context, ordered by version descending
4. Compare two snapshots and return a diff description
5. Extend MyBatis-Plus `IService<SfContextSnapshot>` for standard CRUD operations

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `createSnapshot(Long contextId, String snapshotJson)` | Save a new snapshot of context state | `contextId` — context primary key; `snapshotJson` — serialized state | `SfContextSnapshot` |
| `restoreFromSnapshot(Long snapshotId)` | Restore context state from a snapshot | `snapshotId` — snapshot primary key | `String` (restored JSON) |
| `listSnapshotsByContext(Long contextId)` | List all snapshots for a context (version desc) | `contextId` — context primary key | `List<SfContextSnapshot>` |
| `compareSnapshots(Long snapshotIdA, Long snapshotIdB)` | Compare two snapshots and return diff | `snapshotIdA`, `snapshotIdB` — snapshot primary keys | `String` (diff description) |

## Key Code

```java
public interface ContextSnapshotService extends IService<SfContextSnapshot> {

    SfContextSnapshot createSnapshot(Long contextId, String snapshotJson);

    String restoreFromSnapshot(Long snapshotId);

    List<SfContextSnapshot> listSnapshotsByContext(Long contextId);

    String compareSnapshots(Long snapshotIdA, Long snapshotIdB);
}
```

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `SfContextSnapshot` | Snapshot entity (contextId, snapshotJson, version) |
| `IService<SfContextSnapshot>` | MyBatis-Plus base CRUD interface |

## Backlinks

- Related: [[services/context-service]] — manages the contexts being snapshotted
- Related: [[services/workspace-service]] — workspaces contain contexts with snapshots
- Entity: [[entities/context]]
