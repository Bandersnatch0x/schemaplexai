# MAF Phase 1 Wave 2 + Wave 3 Implementation Tasks

## Wave 2: Checkpoint Hash Enhancement

1. Add `snapshotHash` field to `SfAgentExecutionSnapshot` entity (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/entity/SfAgentExecutionSnapshot.java`)
2. Add `snapshot_hash` column to DB schema via Flyway migration (`schemaplexai-agent-engine/src/main/resources/db/migration/V2026_05_08__add_snapshot_hash.sql`)
3. Compute SHA-256 hash in `AgentExecutionLifecycleService.saveSnapshot()` (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/lifecycle/AgentExecutionLifecycleService.java`)
4. Verify hash in `ResumingStateHandler.handle()` and `getLatestSnapshot()` (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ResumingStateHandler.java` and `AgentExecutionLifecycleService.java`)
5. Write unit tests for hash computation and verification (`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/lifecycle/AgentExecutionLifecycleServiceTest.java`)

## Wave 3: Progressive Skill Disclosure

6. Add `tier` field to `SfAgentSkill` entity (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/entity/SfAgentSkill.java`)
7. Add `tier` column to DB schema via Flyway migration (`schemaplexai-agent-engine/src/main/resources/db/migration/V2026_05_08__add_skill_tier.sql`)
8. Add `tier` to `SkillDefinition` record (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/skill/SkillDefinition.java`)
9. Add `resolveByTier()` / `resolveAvailable()` to `SkillRegistry` (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/skill/SkillRegistry.java`)
10. Track execution maturity (round count) in metadata and modify `ThinkingStateHandler.buildPrompt()` to filter skills by tier (`schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ThinkingStateHandler.java`)
11. Write unit tests for tier-based skill resolution (`schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/skill/SkillRegistryTest.java`)
