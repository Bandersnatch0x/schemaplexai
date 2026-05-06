package com.schemaplexai.agent.engine.security;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtSseTokenValidator} covering all validation paths.
 * Uses a known HMAC-SHA key to produce real JWT tokens for end-to-end validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtSseTokenValidator")
class JwtSseTokenValidatorTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-at-least-32-bytes-long";
    private static final String EXECUTION_ID = "exec-abc-123";
    private static final long EXPIRATION_MS = 3600_000L; // 1 hour

    private JwtSseTokenValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JwtSseTokenValidator();
        // Inject the secret via reflection since we're not in a Spring context
        injectSecret(validator, SECRET);
        validator.initializeKey();
    }

    // ---- Helper methods ----

    private void injectSecret(JwtSseTokenValidator validator, String secret) {
        try {
            var field = JwtSseTokenValidator.class.getDeclaredField("jwtSecret");
            field.setAccessible(true);
            field.set(validator, secret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject jwtSecret", e);
        }
    }

    private String createToken(String subject, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private String createValidToken(String executionId) {
        Date expiry = new Date(System.currentTimeMillis() + EXPIRATION_MS);
        return createToken(executionId, expiry);
    }

    // ---- Null / blank input tests ----

    @Nested
    @DisplayName("Null and blank inputs")
    class NullAndBlankInputs {

        @Test
        @DisplayName("rejects null token")
        void rejectsNullToken() {
            ValidationResult result = validator.validate(null, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is required", result.errorMessage());
        }

        @Test
        @DisplayName("rejects blank token")
        void rejectsBlankToken() {
            ValidationResult result = validator.validate("   ", EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is required", result.errorMessage());
        }

        @Test
        @DisplayName("rejects empty token")
        void rejectsEmptyToken() {
            ValidationResult result = validator.validate("", EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is required", result.errorMessage());
        }

        @Test
        @DisplayName("rejects null execution ID")
        void rejectsNullExecutionId() {
            String token = createValidToken(EXECUTION_ID);
            ValidationResult result = validator.validate(token, null);
            assertFalse(result.isValid());
            assertEquals("Execution ID is required", result.errorMessage());
        }

        @Test
        @DisplayName("rejects blank execution ID")
        void rejectsBlankExecutionId() {
            String token = createValidToken(EXECUTION_ID);
            ValidationResult result = validator.validate(token, "   ");
            assertFalse(result.isValid());
            assertEquals("Execution ID is required", result.errorMessage());
        }
    }

    // ---- Successful validation ----

    @Nested
    @DisplayName("Successful validation")
    class SuccessfulValidation {

        @Test
        @DisplayName("accepts a valid token with matching subject")
        void acceptsValidTokenWithMatchingSubject() {
            String token = createValidToken(EXECUTION_ID);
            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertTrue(result.isValid(), "Should pass with valid signature and matching subject");
        }

        @Test
        @DisplayName("accepts a valid token with UUID subject")
        void acceptsUuidSubject() {
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            String token = createValidToken(uuid);
            ValidationResult result = validator.validate(token, uuid);
            assertTrue(result.isValid());
        }
    }

    // ---- Signature / malformed token tests ----

    @Nested
    @DisplayName("Signature and token format")
    class SignatureAndFormat {

        @Test
        @DisplayName("rejects a token signed with a different key")
        void rejectsTokenSignedWithDifferentKey() {
            String otherSecret = "another-secret-key-that-is-also-at-least-32-bytes!!";
            SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject(EXECUTION_ID)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(otherKey)
                    .compact();

            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token signature is invalid", result.errorMessage());
        }

        @Test
        @DisplayName("rejects a completely malformed string")
        void rejectsMalformedString() {
            ValidationResult result = validator.validate("not-a-jwt-at-all", EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is malformed or invalid", result.errorMessage());
        }

        @Test
        @DisplayName("rejects an unsigned token (alg=none)")
        void rejectsUnsignedToken() {
            // A bare JWT with no signature
            String unsignedToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJleGVjLWFiYy0xMjMifQ.";
            ValidationResult result = validator.validate(unsignedToken, EXECUTION_ID);
            assertFalse(result.isValid());
        }
    }

    // ---- Expiry tests ----

    @Nested
    @DisplayName("Token expiry")
    class TokenExpiry {

        @Test
        @DisplayName("rejects an expired token")
        void rejectsExpiredToken() {
            Date pastExpiry = new Date(System.currentTimeMillis() - 3600_000L); // 1 hour ago
            String token = createToken(EXECUTION_ID, pastExpiry);

            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token has expired", result.errorMessage());
        }

        @Test
        @DisplayName("rejects a just-expired token")
        void rejectsJustExpiredToken() {
            // Token that expired 1 second ago
            Date justExpired = new Date(System.currentTimeMillis() - 1);
            String token = createToken(EXECUTION_ID, justExpired);

            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token has expired", result.errorMessage());
        }
    }

    // ---- Subject mismatch tests ----

    @Nested
    @DisplayName("Subject matching")
    class SubjectMatching {

        @Test
        @DisplayName("rejects a token whose subject does not match execution ID")
        void rejectsSubjectMismatch() {
            String token = createValidToken("other-execution-id");
            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token subject does not match execution ID", result.errorMessage());
        }

        @Test
        @DisplayName("rejects a token with null subject")
        void rejectsNullSubject() {
            // Token without a 'sub' claim
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(key)
                    .compact();

            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is missing subject claim", result.errorMessage());
        }

        @Test
        @DisplayName("rejects a token with empty subject")
        void rejectsEmptySubject() {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                    .signWith(key)
                    .compact();

            ValidationResult result = validator.validate(token, EXECUTION_ID);
            assertFalse(result.isValid());
            assertEquals("Token is missing subject claim", result.errorMessage());
        }
    }
}
