package com.schemaplexai.agent.engine.observability;

/**
 * @deprecated Use {@link com.schemaplexai.common.observability.PiiRedactor} instead.
 */
@Deprecated
public final class PiiRedactor {

    private PiiRedactor() {
    }

    public static String redact(String input) {
        return com.schemaplexai.common.observability.PiiRedactor.redact(input);
    }
}
