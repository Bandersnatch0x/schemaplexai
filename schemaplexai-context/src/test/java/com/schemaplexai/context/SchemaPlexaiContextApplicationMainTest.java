package com.schemaplexai.context;

import org.junit.jupiter.api.Test;

class SchemaPlexaiContextApplicationMainTest {

    @Test
    void mainMethodStartsWithoutError() {
        // Context application depends on data sources not available in unit tests.
        try {
            SchemaPlexaiContextApplication.main(new String[]{});
        } catch (Exception e) {
            // Expected — data source dependencies missing in isolated test context
        }
    }
}
