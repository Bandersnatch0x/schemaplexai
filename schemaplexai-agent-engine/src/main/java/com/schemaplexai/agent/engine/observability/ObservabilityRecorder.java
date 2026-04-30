package com.schemaplexai.agent.engine.observability;

import com.schemaplexai.agent.engine.mapper.ObservabilitySpanMapper;
import com.schemaplexai.agent.engine.mapper.ObservabilityTraceMapper;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObservabilityRecorder {

    private final ObservabilityTraceMapper traceMapper;
    private final ObservabilitySpanMapper spanMapper;

    @Transactional
    public ObservabilityTrace startTrace(String executionId, String name,
                                          String userId, String sessionId, String input) {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId(UUID.randomUUID().toString());
        trace.setName(name + "-" + executionId);
        trace.setUserId(userId);
        trace.setSessionId(sessionId);
        trace.setInput(input);
        traceMapper.insert(trace);
        return trace;
    }

    @Transactional
    public void endTrace(String traceId, String output) {
        ObservabilityTrace trace = traceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ObservabilityTrace>()
                .eq(ObservabilityTrace::getTraceId, traceId));
        if (trace != null) {
            trace.setOutput(output);
            traceMapper.updateById(trace);
        }
    }

    @Transactional
    public ObservabilitySpan addSpan(String traceId, String parentSpanId,
                                      String name, String type,
                                      Long startTime, Long endTime,
                                      String input, String output, String status) {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId(UUID.randomUUID().toString());
        span.setTraceId(traceId);
        span.setParentSpanId(parentSpanId);
        span.setName(name);
        span.setType(type);
        span.setStartTime(startTime);
        span.setEndTime(endTime);
        span.setInput(input);
        span.setOutput(output);
        span.setStatus(status);
        spanMapper.insert(span);
        return span;
    }
}
