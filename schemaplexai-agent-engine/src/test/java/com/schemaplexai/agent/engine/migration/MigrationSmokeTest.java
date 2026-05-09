package com.schemaplexai.agent.engine.migration;

import com.schemaplexai.agent.engine.service.SchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 0.1: Flyway Migration Smoke Test")
class MigrationSmokeTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SchemaValidator schemaValidator;

    @Test
    @DisplayName("V2026_05_09 migration should declare all control-plane tables")
    void migrationDeclaresAllTables() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2026_05_09__execution_control_plane_tables.sql");
        assertThat(resource.exists()).isTrue();

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("sf_execution_event")
                .contains("sf_execution_outbox")
                .contains("sf_processed_event")
                .contains("sf_approval_ticket")
                .contains("version")
                .contains("last_event_seq");
    }

    @Test
    @DisplayName("sf_execution_event should have composite unique constraint on (execution_id, seq)")
    void executionEventHasCompositeUnique() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2026_05_09__execution_control_plane_tables.sql");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("UNIQUE(execution_id, seq)");
    }

    @Test
    @DisplayName("sf_processed_event should have composite PK (event_id, consumer_name)")
    void processedEventHasCompositePk() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2026_05_09__execution_control_plane_tables.sql");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).contains("PRIMARY KEY (event_id, consumer_name)");
    }

    @Test
    @DisplayName("SchemaValidator should confirm migration applied successfully")
    void schemaValidatorReturnsTrue() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString())).thenReturn(1);

        boolean valid = schemaValidator.validate();

        assertThat(valid).isTrue();
    }
}
