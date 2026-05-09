# Phase 0.3: Minimum RED Test Skeleton

## Status: COMPLETE

All 7 RED tests compile and fail at runtime, establishing the behavioral contracts for Phase 1 implementation.

## Test Inventory

| # | Test Class | Module | Tests | Failures | Errors | Contract Verified |
|---|-----------|--------|-------|----------|--------|-------------------|
| 1 | `MigrationSmokeTest` | agent-engine | 4 | 0 | 1 | Flyway migration declares all control-plane tables; SchemaValidator confirms applied schema |
| 2 | `OutboxAtomicWriteTest` | agent-engine | 2 | 2 | 0 | `ExecutionEvent` + `ExecutionOutbox` written atomically in same transaction |
| 3 | `ExecutionVersionConflictTest` | agent-engine | 3 | 3 | 0 | Optimistic locking rejects stale `expectedVersion`; increments version on success |
| 4 | `EventReorderingTest` | agent-engine | 3 | 0 | 3 | Out-of-order events buffered and applied in `seq` order; gap fill triggers release |
| 5 | `ApprovalRequestIdempotencyTest` | quality | 3 | 2 | 1 | Duplicate `approvalRequestId` ignored; multiple approvals per execution allowed with different `triggeringSeq` |
| 6 | `ApprovalDecisionVersionTest` | quality | 3 | 3 | 0 | `ApprovalDecisionEvent` validates `expectedExecutionVersion` and `decisionVersion` |
| 7 | `SseReplayTest` | web | 3 | 3 | 0 | SSE replay from `lastSeq` excludes `EPHEMERAL`; includes `AUDIT` and `DEBUG` |

**Total: 21 test methods, 13 failures, 5 errors = all RED**

## Files Created

### Migration
- `schemaplexai-agent-engine/src/main/resources/db/migration/V2026_05_09__execution_control_plane_tables.sql`

### Entities
- `schemaplexai-agent-engine/.../entity/ExecutionEvent.java`
- `schemaplexai-agent-engine/.../entity/ExecutionOutbox.java`
- `schemaplexai-agent-engine/.../entity/ProcessedEvent.java`
- `schemaplexai-quality/.../entity/ApprovalTicket.java`

### Mappers
- `schemaplexai-agent-engine/.../mapper/ExecutionEventMapper.java`
- `schemaplexai-agent-engine/.../mapper/ExecutionOutboxMapper.java`
- `schemaplexai-agent-engine/.../mapper/ProcessedEventMapper.java`
- `schemaplexai-quality/.../mapper/ApprovalTicketMapper.java`

### Shared Events (model module)
- `schemaplexai-model/.../event/ApprovalRequestEvent.java`
- `schemaplexai-model/.../event/ApprovalDecisionEvent.java`

### Service Stubs (throw UnsupportedOperationException or return wrong values)
- `schemaplexai-agent-engine/.../service/ExecutionEventService.java`
- `schemaplexai-agent-engine/.../service/ExecutionConcurrencyService.java`
- `schemaplexai-agent-engine/.../service/ExecutionEventBuffer.java`
- `schemaplexai-agent-engine/.../service/SchemaValidator.java`
- `schemaplexai-quality/.../service/ApprovalRequestConsumer.java`
- `schemaplexai-quality/.../service/ApprovalDecisionValidator.java`
- `schemaplexai-web/.../service/SseReplayService.java`
- `schemaplexai-web/.../dto/SseEvent.java`

### Modified
- `SfAgentExecution` — added `version` and `lastEventSeq` fields

## Verification Commands

```bash
# Agent-engine RED tests
mvn test -pl schemaplexai-agent-engine -Dtest="MigrationSmokeTest,OutboxAtomicWriteTest,ExecutionVersionConflictTest,EventReorderingTest"

# Quality RED tests
mvn test -pl schemaplexai-quality -Dtest="ApprovalRequestIdempotencyTest,ApprovalDecisionVersionTest"

# Web RED tests
mvn test -pl schemaplexai-web -Dtest="SseReplayTest"
```

## Phase 0 Completion Criteria

- [x] 0.1 Schema Owner & Migration Strategy — `schema-ownership.md`
- [x] 0.2 Module Ownership & Dependencies — `module-ownership.md` + AMQP in quality
- [x] 0.3 Minimum RED Test Skeleton — `red-test-skeleton.md` + 21 RED tests
