package com.schemaplexai.agent.engine.chain;

import java.util.List;
import java.util.Map;

/**
 * Defines a complete prompt chain consisting of ordered steps.
 *
 * @param id            unique chain identifier
 * @param name          human-readable chain name
 * @param description   chain description
 * @param steps         ordered list of steps to execute
 * @param initialInputs initial template variable values available to the first step
 */
public record ChainDefinition(
        String id,
        String name,
        String description,
        List<ChainStep> steps,
        Map<String, Object> initialInputs
) {
}
