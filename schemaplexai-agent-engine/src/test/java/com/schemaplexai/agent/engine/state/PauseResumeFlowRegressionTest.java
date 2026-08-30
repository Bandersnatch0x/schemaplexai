package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshotPersistence;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue 907 / REQ-07 regression: the pause&rarr;resume round trip must work on real
 * identifiers.
 *
 * <p>Before the fix, PAUSED stored the executionId as {@code execution.snapshotId}
 * while the snapshot row lives under its own generated primary key — so RESUMING's
 * {@code selectById(snapshotId)} could never find the row and every resume failed
 * with "Snapshot not found". The corrected wiring:</p>
 * <ol>
 *   <li>pause persists the snapshot and stores the GENERATED ROW KEY on the execution;</li>
 *   <li>resume loads the snapshot by that exact key and restores context;</li>
 *   <li>a missing snapshot fails explicitly (reason recorded, FAILED state).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class PauseResumeFlowRegressionTest {

    private static final long SNAPSHOT_ROW_ID = 424242L;

    @Mock
    private ExecutionSnapshotPersistence snapshotPersistence;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private SfAgentExecutionSnapshotMapper snapshotMapper;

    @Mock
    private AgentStateMachine stateMachine;

    private PausedStateHandler pausedHandler;
    private ResumingStateHandler resumingHandler;
    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        pausedHandler = new PausedStateHandler(snapshotPersistence, chatMemoryStore, new ObjectMapper());
        resumingHandler = new ResumingStateHandler(snapshotMapper);

        execution = new SfAgentExecution();
        execution.setId(777L);
        execution.setAgentId(42L);
        execution.setConversationId("conv-pause-resume");
        execution.setState(AgentExecutionState.PAUSED.name());
    }

    @Test
    void pauseThenResumeShouldRestoreContextViaRealSnapshotRowKey() {
        // --- PAUSED: snapshot persisted, generated row key stored on the execution ---
        when(chatMemoryStore.loadMessages("conv-pause-resume"))
                .thenReturn(List.of(new LlmMessage("user", "hello"), new LlmMessage("assistant", "hi")));
        when(snapshotPersistence.saveSnapshot(any(ExecutionSnapshot.class))).thenReturn(SNAPSHOT_ROW_ID);

        pausedHandler.handle(stateMachine, execution);

        // The corrected identifier: the snapshot ROW key, not the execution id.
        assertEquals(SNAPSHOT_ROW_ID, execution.getSnapshotId());
        verify(stateMachine).saveExecution(execution);

        // --- RESUMING: load by the same row key and restore context ---
        String snapshotJson = "{\"executionId\":777,\"state\":\"PAUSED\"}";
        SfAgentExecutionSnapshot row = new SfAgentExecutionSnapshot();
        row.setId(SNAPSHOT_ROW_ID);
        row.setExecutionId(777L);
        row.setSnapshotJson(snapshotJson);
        row.setSnapshotHash(HashUtils.sha256(snapshotJson));
        when(snapshotMapper.selectById(SNAPSHOT_ROW_ID)).thenReturn(row);

        resumingHandler.handle(stateMachine, execution);

        assertEquals(snapshotJson, execution.getMetadata("restoredContext"));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void resumeShouldFailExplicitlyWhenSnapshotRowIsMissing() {
        execution.setSnapshotId(SNAPSHOT_ROW_ID);
        when(snapshotMapper.selectById(SNAPSHOT_ROW_ID)).thenReturn(null);

        resumingHandler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(stateMachine, never()).transition(AgentExecutionState.THINKING, execution);
        Object reason = execution.getMetadata("failureReason");
        assertNotNull(reason, "missing snapshot must carry an explicit failure reason");
        assertTrue(reason.toString().contains("Resume failed"));
        assertTrue(reason.toString().contains("not found"));
        assertTrue(reason.toString().contains(String.valueOf(SNAPSHOT_ROW_ID)));
    }
}
