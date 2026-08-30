package com.schemaplexai.agent.engine.lifecycle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.service.ExecutionSnapshotService;
import com.schemaplexai.agent.engine.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionSnapshotPersistence {

    private final SfAgentExecutionSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;
    private final ExecutionSnapshotService executionSnapshotService;

    /**
     * Persist the snapshot and return the generated snapshot-row primary key.
     *
     * <p>Issue 907 / REQ-07: the resume path must load the snapshot by the actual
     * row key of {@code sf_agent_execution_snapshot}. MyBatis-Plus back-fills the
     * {@code ASSIGN_ID} primary key onto the entity during insert; returning it lets
     * the pause path store the real identifier on the execution. Returning void (the
     * old behavior) discarded the generated key, so {@code execution.snapshotId} was
     * left pointing at the executionId — a value the snapshot table's own BIGSERIAL /
     * snowflake primary-key space never contains, making every resume fail with
     * "Snapshot not found".</p>
     *
     * @return the generated snapshot row id, or null if no id was assigned
     * @throws IllegalStateException when the snapshot cannot be serialized
     */
    public Long saveSnapshot(ExecutionSnapshot snapshot) {
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            // Fail explicitly: a pause whose snapshot cannot be serialized must not
            // pretend to be resumable.
            throw new IllegalStateException(
                    "Failed to serialize execution snapshot for execution "
                            + snapshot.getExecutionId(), e);
        }

        SfAgentExecutionSnapshot entity = new SfAgentExecutionSnapshot();
        entity.setExecutionId(snapshot.getExecutionId());
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotHash(HashUtils.sha256(snapshotJson));
        entity.setTenantId(null);
        snapshotMapper.insert(entity);

        executionSnapshotService.saveSnapshot(snapshot.getExecutionId(), snapshotJson, 0);

        // Generated snapshot-row primary key (ASSIGN_ID) — the identifier that the
        // RESUMING handler must load by.
        return entity.getId();
    }

    public ExecutionSnapshot getLatestSnapshot(Long executionId) {
        String cachedJson = executionSnapshotService.restoreSnapshot(executionId);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                return objectMapper.readValue(cachedJson, ExecutionSnapshot.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached snapshot for execution {}, falling back to legacy DB",
                        executionId, e);
            }
        }

        SfAgentExecutionSnapshot entity = snapshotMapper.selectOne(
                new LambdaQueryWrapper<SfAgentExecutionSnapshot>()
                        .eq(SfAgentExecutionSnapshot::getExecutionId, executionId)
                        .orderByDesc(SfAgentExecutionSnapshot::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (entity == null || entity.getSnapshotJson() == null) {
            return null;
        }

        String snapshotJson = entity.getSnapshotJson();
        String storedHash = entity.getSnapshotHash();
        if (storedHash != null && !storedHash.isBlank()) {
            String computedHash = HashUtils.sha256(snapshotJson);
            if (!HashUtils.constantTimeEquals(storedHash, computedHash)) {
                log.error("Snapshot hash mismatch for execution {}. Data may have been tampered.", executionId);
                return null;
            }
        } else {
            log.warn("Snapshot hash missing for execution {} (legacy data). Allowing through without integrity check.",
                    executionId);
        }

        try {
            return objectMapper.readValue(snapshotJson, ExecutionSnapshot.class);
        } catch (Exception e) {
            log.error("Failed to deserialize snapshot for execution {}", executionId, e);
            return null;
        }
    }
}
