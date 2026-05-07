package com.schemaplexai.agent.engine.guardrails;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration for guardrails components.
 * Wires the GuardrailsEngine with all registered guardrail rules.
 */
@Configuration
public class GuardrailsConfig {

    @Bean
    public BlacklistGuardrail blacklistGuardrail() {
        return new BlacklistGuardrail();
    }

    @Bean
    public LengthGuardrail lengthGuardrail() {
        return new LengthGuardrail();
    }

    @Bean
    public GuardrailsEngine guardrailsEngine(BlacklistGuardrail blacklistGuardrail,
                                              LengthGuardrail lengthGuardrail) {
        return new GuardrailsEngine(List.of(blacklistGuardrail, lengthGuardrail));
    }
}
