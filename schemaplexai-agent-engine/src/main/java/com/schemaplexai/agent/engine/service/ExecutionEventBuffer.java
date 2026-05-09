package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * In-order event application with gap detection and recovery.
 * Stateful per execution instance.
 */
@Service
public class ExecutionEventBuffer {

    private int confirmedSeq = 0;
    private final TreeMap<Integer, ExecutionEvent> buffer = new TreeMap<>();

    public List<ExecutionEvent> applyInOrder(List<ExecutionEvent> events) {
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
