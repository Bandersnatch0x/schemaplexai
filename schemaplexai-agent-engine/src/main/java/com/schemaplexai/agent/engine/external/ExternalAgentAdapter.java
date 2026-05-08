package com.schemaplexai.agent.engine.external;

/**
 * SPI for external agent adapters.
 * Defines the lifecycle and communication contract for third-party agent providers.
 */
public interface ExternalAgentAdapter {

    /**
     * Initialize the adapter and establish connection to the external agent.
     */
    void start();

    /**
     * Send a message or task to the external agent.
     *
     * @param message the payload to send
     * @return the event response from the external agent
     */
    AgentEvent send(String message);

    /**
     * Interrupt the current operation of the external agent.
     */
    void interrupt();

    /**
     * Release resources and close the connection.
     */
    void close();
}
