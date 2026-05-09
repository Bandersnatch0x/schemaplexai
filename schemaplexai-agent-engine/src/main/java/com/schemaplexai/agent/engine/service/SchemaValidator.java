package com.schemaplexai.agent.engine.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.agent.engine.dto.SchemaValidationError;
import com.schemaplexai.agent.engine.dto.SchemaValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates execution control plane schema after migration.
 * Caches metadata queries to avoid repeated information_schema access.
 */
@Service
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    private static final List<String> REQUIRED_TABLES = List.of(
            "sf_execution_event",
            "sf_execution_outbox",
            "sf_processed_event",
            "sf_approval_ticket"
    );

    private static final String TABLE_CHECK_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
    private static final String COLUMN_CHECK_SQL =
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?";

    private final JdbcTemplate jdbcTemplate;

    /** Cache schema validation results with 5-minute TTL to avoid repeated information_schema queries. */
    private final Cache<String, SchemaValidationResult> validationCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /** Cache individual table existence checks with 2-minute TTL. */
    private final Cache<String, Boolean> tableExistenceCache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();

    public SchemaValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Validates that all required control-plane tables and columns exist.
     * Results are cached with a 5-minute TTL to prevent repeated information_schema queries.
     *
     * @return true if all tables and columns are present
     */
    public boolean validate() {
        return validateDetailed().isValid();
    }

    /**
     * Validates and returns detailed results including per-table/per-column error reporting.
     *
     * @return SchemaValidationResult with validation status and any errors
     */
    public SchemaValidationResult validateDetailed() {
        SchemaValidationResult cached = validationCache.getIfPresent("schema");
        if (cached != null) {
            log.debug("Returning cached schema validation result: valid={}", cached.isValid());
            return cached;
        }

        log.info("Running schema validation against information_schema");
        Instant start = Instant.now();
        List<SchemaValidationError> errors = new ArrayList<>();

        if (jdbcTemplate == null) {
            log.warn("JdbcTemplate is null, skipping schema validation");
            SchemaValidationResult result = new SchemaValidationResult(true, List.of());
            validationCache.put("schema", result);
            return result;
        }

        try {
            for (String table : REQUIRED_TABLES) {
                Boolean exists = tableExistenceCache.get(table, t -> {
                    try {
                        Integer count = jdbcTemplate.queryForObject(TABLE_CHECK_SQL, Integer.class, t);
                        return count != null && count > 0;
                    } catch (DataAccessException e) {
                        log.error("Failed to check table existence for '{}': {}", t, e.getMessage());
                        return false;
                    }
                });

                if (Boolean.FALSE.equals(exists)) {
                    String message = "Required table '" + table + "' does not exist in information_schema";
                    log.error(message);
                    errors.add(new SchemaValidationError("TABLE_MISSING", message));
                }
            }

            // Check required columns on sf_agent_execution
            checkColumn("sf_agent_execution", "version", errors);
            checkColumn("sf_agent_execution", "last_event_seq", errors);

        } catch (DataAccessException e) {
            String message = "Schema validation failed due to database error: " + e.getMessage();
            log.error(message, e);
            errors.add(new SchemaValidationError("DB_ERROR", message));
        }

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        boolean valid = errors.isEmpty();
        log.info("Schema validation completed in {}ms: valid={}, errors={}", durationMs, valid, errors.size());

        SchemaValidationResult result = new SchemaValidationResult(valid, errors);
        validationCache.put("schema", result);
        return result;
    }

    private void checkColumn(String tableName, String columnName, List<SchemaValidationError> errors) {
        try {
            Integer count = jdbcTemplate.queryForObject(COLUMN_CHECK_SQL, Integer.class, tableName, columnName);
            if (count == null || count == 0) {
                String message = "Required column '" + columnName + "' missing from table '" + tableName + "'";
                log.error(message);
                errors.add(new SchemaValidationError("COLUMN_MISSING", message));
            }
        } catch (DataAccessException e) {
            String message = "Failed to check column '" + columnName + "' on '" + tableName + "': " + e.getMessage();
            log.error(message, e);
            errors.add(new SchemaValidationError("DB_ERROR", message));
        }
    }
}