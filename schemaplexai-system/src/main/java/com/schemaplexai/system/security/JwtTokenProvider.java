package com.schemaplexai.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @PostConstruct
    public void validateJwtSecret() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long. Please set the JWT_SECRET environment variable.");
        }
    }

    /**
     * Generates an access token with the default expiration.
     */
    public String generateToken(String userId, String tenantId, String username) {
        return generateToken(userId, tenantId, username, jwtExpiration);
    }

    /**
     * Generates a token with an explicit expiration duration (in milliseconds).
     * Used by AuthService for refresh tokens.
     */
    public String generateToken(String userId, String tenantId, long expirationMs) {
        return buildToken(userId, tenantId, expirationMs);
    }

    /**
     * Generates a token with an explicit expiration duration and username claim.
     */
    public String generateToken(String userId, String tenantId, String username, long expirationMs) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .id(jti)
                .subject(userId)
                .claim("tenantId", tenantId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private String buildToken(String userId, String tenantId, long expirationMs) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .id(jti)
                .subject(userId)
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getJti(String token) {
        return parseToken(token).getId();
    }

    public Date getExpirationDate(String token) {
        return parseToken(token).getExpiration();
    }

    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
