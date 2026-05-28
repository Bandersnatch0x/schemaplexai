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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @DisplayName("Flyway migration versions should be unique")
    void flywayMigrationVersionsAreUnique() throws IOException {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");

        Map<String, Long> versionCounts = Files.list(migrationDirectory)
                .map(path -> path.getFileName().toString())
                .filter(filename -> filename.startsWith("V") && filename.endsWith(".sql"))
                .collect(Collectors.groupingBy(this::migrationVersion, Collectors.counting()));

        assertThat(versionCounts)
                .allSatisfy((version, count) -> assertThat(count)
                        .as("migration version %s appears more than once", version)
                        .isEqualTo(1));
    }

    @Test
    @DisplayName("Flyway migration versions should be unique across default classpath")
    void flywayMigrationVersionsAreUniqueAcrossDefaultClasspath() throws IOException {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize();

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            Map<String, List<String>> migrationsByVersion = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString()
                            .replace('\\', '/')
                            .contains("/src/main/resources/db/migration/"))
                    .map(repoRoot::relativize)
                    .map(Path::toString)
                    .filter(filename -> filename.endsWith(".sql"))
                    .filter(path -> Path.of(path).getFileName().toString().startsWith("V"))
                    .collect(Collectors.groupingBy(
                            path -> migrationVersion(Path.of(path).getFileName().toString()),
                            Collectors.toList()
                    ));

            assertThat(migrationsByVersion)
                    .allSatisfy((version, pathsForVersion) -> assertThat(pathsForVersion)
                            .as("migration version %s appears in %s", version, pathsForVersion)
                            .hasSize(1));
        }
    }

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

    private String migrationVersion(String filename) {
        return filename.substring(1, filename.indexOf("__"));
    }
}
