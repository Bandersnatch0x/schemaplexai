package com.schemaplexai.agent.engine.loop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AgentLoopDetectionService {

    private final int windowSize;
    private final int maxSameHash;
    private final int maxSameToolSequence;

    private final Map<Long, List<RoundRecord>> executionRecords = new ConcurrentHashMap<>();

    public AgentLoopDetectionService(
            @Value("${agent.loop-detection.window-size:5}") int windowSize,
            @Value("${agent.loop-detection.max-same-hash:3}") int maxSameHash,
            @Value("${agent.loop-detection.max-same-tool-sequence:3}") int maxSameToolSequence) {
        this.windowSize = windowSize;
        this.maxSameHash = maxSameHash;
        this.maxSameToolSequence = maxSameToolSequence;
    }

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
            log.warn("Loop detected in execution {}: hash repeated {} times in window of {}",
                    executionId, sameHashCount, windowSize);
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
                log.warn("Loop detected in execution {}: tool sequence repeated {} times in window of {}",
                        executionId, sameSequenceCount, windowSize);
                return LoopDetectionResult.toolSequenceLoop();
            }
        }

        return LoopDetectionResult.noLoop();
    }

    public void recordRound(Long executionId, String responseHash, List<String> toolNames) {
        executionRecords.computeIfAbsent(executionId, k -> new ArrayList<>())
                .add(new RoundRecord(responseHash, toolNames != null ? new ArrayList<>(toolNames) : Collections.emptyList()));
    }

    public void clearRecords(Long executionId) {
        executionRecords.remove(executionId);
    }

    public int getRecordCount(Long executionId) {
        return executionRecords.getOrDefault(executionId, Collections.emptyList()).size();
    }

    record RoundRecord(String responseHash, List<String> toolNames) {
    }
}
