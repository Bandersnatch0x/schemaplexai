---
title: AgentLoopDetectionService
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/loop/AgentLoopDetectionService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, agent, loop-detection, execution, safety]
confidence: high
---

# AgentLoopDetectionService

> One-sentence summary: Detects repetitive patterns in agent execution rounds — hash repetition and tool-sequence repetition — to prevent infinite loops.

## Responsibilities

1. Record execution rounds (response hash + tool names) per execution ID
2. Detect hash loops: same response hash repeated within a sliding window
3. Detect tool-sequence loops: same tool call sequence repeated within a sliding window
4. Maintain in-memory execution history with ConcurrentHashMap
5. Provide manual record clearing and count inspection

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `detectLoop(Long executionId, String responseHash, List<String> toolNames)` | Analyze latest round for loop patterns | `executionId` — execution ID; `responseHash` — hash of agent response; `toolNames` — ordered list of tools called | `LoopDetectionResult` |
| `recordRound(Long executionId, String responseHash, List<String> toolNames)` | Append a round record to execution history | `executionId`, `responseHash`, `toolNames` | void |
| `clearRecords(Long executionId)` | Remove all records for an execution | `executionId` — execution ID | void |
| `getRecordCount(Long executionId)` | Return number of recorded rounds | `executionId` — execution ID | `int` |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `agent.loop-detection.window-size` | 5 | Sliding window size for loop detection |
| `agent.loop-detection.max-same-hash` | 3 | Threshold for hash-loop detection |
| `agent.loop-detection.max-same-tool-sequence` | 3 | Threshold for tool-sequence-loop detection |

## Key Code

```java
public LoopDetectionResult detectLoop(Long executionId, String responseHash, List<String> toolNames) {
    recordRound(executionId, responseHash, toolNames);

    List<RoundRecord> rounds = executionRecords.getOrDefault(executionId, Collections.emptyList());
    if (rounds.size() < windowSize) {
        return LoopDetectionResult.noLoop();
    }

    List<RoundRecord> window = rounds.subList(rounds.size() - windowSize, rounds.size());

    // Hash loop detection
    int sameHashCount = 0;
    for (RoundRecord round : window) {
        if (Objects.equals(round.responseHash(), responseHash)) {
            sameHashCount++;
        }
    }
    if (sameHashCount >= maxSameHash) {
        return LoopDetectionResult.hashLoop();
    }

    // Tool sequence loop detection
    if (toolNames != null && !toolNames.isEmpty()) {
        int sameSequenceCount = 0;
        for (RoundRecord round : window) {
            if (round.toolNames().equals(toolNames)) {
                sameSequenceCount++;
            }
        }
        if (sameSequenceCount >= maxSameToolSequence) {
            return LoopDetectionResult.toolSequenceLoop();
        }
    }

    return LoopDetectionResult.noLoop();
}
```

## LoopDetectionResult

Defined in `LoopDetectionResult.java`:
- `noLoop()` — `loopDetected=false`, `reason=null`
- `hashLoop()` — `loopDetected=true`, `reason=HASH_LOOP`
- `toolSequenceLoop()` — `loopDetected=true`, `reason=TOOL_SEQUENCE_LOOP`

## Dependencies / Collaborators

| Component | Role |
|-----------|------|
| `LoopDetectionResult` | Immutable record returning detection outcome |
| `RoundRecord` | Private record storing response hash and tool names per round |

## Backlinks

- Related: [[services/agent-runtime-orchestrator]] — likely invokes loop detection during execution
- Related: [[services/agent-state-machine]] — may transition to FAILED on loop detection
- Related: [[services/agent-execution-lifecycle-service]] — may clear records on cancel
