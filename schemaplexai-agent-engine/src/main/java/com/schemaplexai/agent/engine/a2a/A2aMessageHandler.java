package com.schemaplexai.agent.engine.a2a;

/**
 * Handler interface for processing incoming A2A messages.
 * Implementations should be registered as Spring components and declare
 * which {@link A2aMessage.MessageType} they support.
 */
public interface A2aMessageHandler {

    /**
     * Process an incoming A2A message and return a response.
     *
     * @param message the incoming message
     * @return the response message, or null if no response is required
     */
    A2aMessage handleMessage(A2aMessage message);

    /**
     * Determine whether this handler supports the given message type.
     *
     * @param messageType the message type string (e.g., "REQUEST", "RESPONSE")
     * @return true if this handler can process the message type
     */
    boolean supports(String messageType);
}
