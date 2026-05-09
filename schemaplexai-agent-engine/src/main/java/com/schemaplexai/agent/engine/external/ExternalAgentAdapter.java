package com.schemaplexai.agent.engine.external;

import java.util.List;

/**
 * SPI for external agent adapters.
 * Defines the lifecycle and communication contract for third-party agent providers.
 *
 * <p>Implementations (e.g. {@link com.schemaplexai.agent.engine.external.codex.CodexAdapter})
 * manage a session-oriented conversation with an external agent process or service.
 */
public interface ExternalAgentAdapter {

    /**
     * Start a new session with the external agent.
     *
     * @param sessionId unique identifier for this session
     * @throws com.schemaplexai.common.exception.BaseException if the session cannot be started
     */
    void startSession(String sessionId);

    /**
     * Send a message to the external agent within the current session.
     *
     * @param sessionId the active session identifier
     * @param message   the payload to send
     * @return the event response from the external agent
     * @throws com.schemaplexai.common.exception.BaseException if no session is active or send fails
     */
    AgentEvent sendMessage(String sessionId, String message);

    /**
     * Retrieve all events emitted by the external agent for the given session.
     *
     * @param sessionId the session identifier
     * @return list of events recorded for this session (may be empty)
     */
    List<AgentEvent> getEvents(String sessionId);

    /**
     * Terminate the session and release associated resources.
     *
     * @param sessionId the session identifier to terminate
     */
    void terminateSession(String sessionId);
}
