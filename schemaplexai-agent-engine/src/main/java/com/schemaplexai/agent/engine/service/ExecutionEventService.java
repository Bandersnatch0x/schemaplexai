package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Append-only event log with outbox atomic write.
 * Delegates MQ publishing to {@link OutboxPublisher}.
 */
@Service
public class ExecutionEventService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEventService.class);

    private final ExecutionEventMapper executionEventMapper;
    private final ExecutionOutboxMapper executionOutboxMapper;

    public ExecutionEventService(ExecutionEventMapper executionEventMapper,
                                 ExecutionOutboxMapper executionOutboxMapper) {
        this.executionEventMapper = executionEventMapper;
        this.executionOutboxMapper = executionOutboxMapper;
    }

    /**
     * Atomically writes ExecutionEvent and ExecutionOutbox in the same transaction.
     * The OutboxPublisher background service will handle MQ delivery.
     */
    @Transactional
    public void appendEventAndOutbox(ExecutionEvent event, String topic) {
        executionEventMapper.insert(event);

        ExecutionOutbox outbox = new ExecutionOutbox();
        outbox.setEventId(event.getEventId());
        outbox.setExecutionId(event.getExecutionId());
        outbox.setSeq(event.getSeq());
        outbox.setTopic(topic);
        outbox.setPayload(event.getPayload());
        outbox.setCreatedAt(Instant.now());
        outbox.setRetryCount(0);

        executionOutboxMapper.insert(outbox);
        log.debug("Appended event {} (seq={}) to outbox with topic '{}'",
                event.getEventId(), event.getSeq(), topic);
    }
}