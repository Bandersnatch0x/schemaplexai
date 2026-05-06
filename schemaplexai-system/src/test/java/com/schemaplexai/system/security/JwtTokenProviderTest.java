package com.schemaplexai.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L);
    }

    @Test
    void generateToken_returnsValidJwt() {
        String token = jwtTokenProvider.generateToken("user-1", "tenant-1", "testuser");

        assertThat(token).isNotBlank();
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }

    @Test
    void generateToken_containsCorrectClaims() {
        String token = jwtTokenProvider.generateToken("user-1", "tenant-1", "testuser");

        Claims claims = jwtTokenProvider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("tenant-1");
        assertThat(claims.get("username", String.class)).isEqualTo("testuser");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void generateToken_expirationIsInFuture() {
        String token = jwtTokenProvider.generateToken("user-1", "tenant-1", "testuser");

        Claims claims = jwtTokenProvider.parseToken(token);

        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void parseToken_returnsCorrectClaims() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .id("test-jti")
                .subject("user-42")
                .claim("tenantId", "tenant-abc")
                .claim("username", "alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        Claims claims = jwtTokenProvider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("user-42");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("tenant-abc");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.getId()).isEqualTo("test-jti");
    }

    @Test
    void getJti_returnsCorrectJti() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .id("unique-jti-123")
                .subject("user-1")
                .claim("tenantId", "tenant-1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        String jti = jwtTokenProvider.getJti(token);

        assertThat(jti).isEqualTo("unique-jti-123");
    }

    @Test
    void getExpirationDate_returnsCorrectDate() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        // JWT exp claim is stored in seconds, so use a date without sub-second precision
        long millis = System.currentTimeMillis() + 7200_000;
        Date expectedExpiry = new Date(millis - (millis % 1000));
        String token = Jwts.builder()
                .id("test-jti")
                .subject("user-1")
                .claim("tenantId", "tenant-1")
                .issuedAt(new Date())
                .expiration(expectedExpiry)
                .signWith(key)
                .compact();

        Date expiry = jwtTokenProvider.getExpirationDate(token);

        assertThat(expiry).isEqualTo(expectedExpiry);
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("user-1", "tenant-1", "testuser");

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("user-1")
                .claim("tenantId", "tenant-1")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        boolean result = jwtTokenProvider.validateToken(expiredToken);

        assertThat(result).isFalse();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        boolean result = jwtTokenProvider.validateToken("not.a.valid.token");

        assertThat(result).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-1", "tenant-1", "testuser");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        boolean result = jwtTokenProvider.validateToken(tampered);

        assertThat(result).isFalse();
    }
}
