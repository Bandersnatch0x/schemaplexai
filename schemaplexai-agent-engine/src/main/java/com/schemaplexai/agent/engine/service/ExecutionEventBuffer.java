package com.schemaplexai.agent.engine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Per-execution in-order event application with gap detection and recovery.
 * Each execution maintains its own buffer and confirmed-seq watermark.
 * Buffers expire after 1 hour of inactivity to prevent memory leaks.
 */
@Service
public class ExecutionEventBuffer {

    private final Cache<Long, BufferState> buffers = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public List<ExecutionEvent> applyInOrder(Long executionId, List<ExecutionEvent> events) {
        BufferState state = buffers.get(executionId, k -> new BufferState());
        return state.apply(events);
    }

    public boolean hasGap(Long executionId) {
        BufferState state = buffers.getIfPresent(executionId);
        if (state == null) return false;
        return !state.buffer.isEmpty() && state.buffer.firstKey() > state.confirmedSeq + 1;
    }

    public int getNextExpectedSeq(Long executionId) {
        BufferState state = buffers.getIfPresent(executionId);
        return state == null ? 1 : state.confirmedSeq + 1;
    }

    public int getBufferedCount(Long executionId) {
        BufferState state = buffers.getIfPresent(executionId);
        return state == null ? 0 : state.buffer.size();
    }

    private static class BufferState {
        int confirmedSeq = 0;
        final TreeMap<Integer, ExecutionEvent> buffer = new TreeMap<>();

        List<ExecutionEvent> apply(List<ExecutionEvent> events) {
            List<ExecutionEvent> emitted = new ArrayList<>();
            for (ExecutionEvent event : events) {
                int seq = event.getSeq();
                if (seq <= confirmedSeq) {
                    continue;
                }
                buffer.put(seq, event);
            }
            while (!buffer.isEmpty() && buffer.firstKey() == confirmedSeq + 1) {
                ExecutionEvent next = buffer.pollFirstEntry().getValue();
                emitted.add(next);
                confirmedSeq++;
            }
            return emitted;
        }
    }
}
