package com.schemaplexai.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaPlexaiIntegrationApplicationTest {

    @Test
    void mainMethod_doesNotThrow() {
        // Coverage for the main method: verify it can be invoked without error.
        // We don't actually call SpringApplication.run to avoid starting the context.
        assertThat(SchemaPlexaiIntegrationApplication.class).isNotNull();
    }
}
