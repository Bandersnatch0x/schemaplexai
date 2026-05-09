package com.schemaplexai.task.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service for manually retrying dead letter events.
 *
 * <p>v1: Stub implementation. Logs retry requests and returns empty lists.
 * Future versions will:
 * <ul>
 *   <li>Query {@code sf_execution_outbox} for DEAD entries by eventId</li>
 *   <li>Republish the event to the execution_events exchange</li>
 *   <li>Support listing dead events with pagination</li>
 * </ul>
 */
@Slf4j
@Service
public class DeadLetterRetryService {

    /**
     * Finds the dead outbox entry for the given eventId and republishes it.
     *
     * <p>v1 stub: logs the retry request. Full implementation will query
     * the outbox table and republish via RabbitTemplate.
     *
     * @param eventId the UUID of the dead event to retry
     */
    public void retryDeadEvent(UUID eventId) {
        log.info("[DeadLetterRetryService] Manual retry requested for dead event: eventId={}", eventId);
        // TODO v2: Query sf_execution_outbox for DEAD entry matching eventId,
        //          then republish via RabbitTemplate to execution_events exchange.
    }

    /**
     * Lists recent dead events.
     *
     * <p>v1 stub: returns an empty list. Full implementation will query
     * sf_audit_event where event_type = 'DEAD_LETTER' or sf_execution_outbox
     * where retry_count >= max_retries.
     *
     * @param limit maximum number of events to return
     * @return list of dead events (empty in v1)
     */
    public List<Object> listDeadEvents(int limit) {
        log.debug("[DeadLetterRetryService] listDeadEvents called with limit={}", limit);
        // TODO v2: Query sf_audit_event or sf_execution_outbox for dead events.
        return Collections.emptyList();
    }
}
