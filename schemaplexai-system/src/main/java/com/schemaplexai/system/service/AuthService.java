package com.schemaplexai.system.service;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validateJwtSecret() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long. Please set the JWT_SECRET environment variable.");
        }
    }

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long jwtRefreshExpiration;

    public Map<String, String> login(String username, String password, String tenantId) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "username or password is empty");
        }

        SfUser user = userService.getByUsernameAndTenantId(username, tenantId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BaseException(ResultCode.PASSWORD_ERROR);
        }

        String accessToken = generateToken(user.getId().toString(), tenantId, jwtExpiration);
        String refreshToken = generateToken(user.getId().toString(), tenantId, jwtRefreshExpiration);

        stringRedisTemplate.opsForValue().set(
                TenantRedisKeyResolver.tokenSession(user.getId().toString()),
                accessToken,
                Duration.ofMillis(jwtExpiration)
        );

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return result;
    }

    public Map<String, String> refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "refresh token is empty");
        }

        if (isTokenBlacklisted(refreshToken)) {
            throw new BaseException(ResultCode.TOKEN_INVALID, "token has been revoked");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            io.jsonwebtoken.Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);

            String newAccessToken = generateToken(userId, tenantId, jwtExpiration);
            String newRefreshToken = generateToken(userId, tenantId, jwtRefreshExpiration);

            Map<String, String> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("refreshToken", newRefreshToken);
            result.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
            return result;
        } catch (Exception e) {
            log.warn("Refresh token invalid: {}", e.getMessage());
            throw new BaseException(ResultCode.TOKEN_INVALID);
        }
    }

    public void logout(String userId) {
        logout(userId, null);
    }

    public void logout(String userId, String token) {
        if (StringUtils.hasText(userId)) {
            stringRedisTemplate.delete(TenantRedisKeyResolver.tokenSession(userId));
        }
        if (StringUtils.hasText(token)) {
            blacklistToken(token);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = jwtTokenProvider.getJti(token);
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey("sf:token:blacklist:" + jti));
        } catch (Exception e) {
            return false;
        }
    }

    public void blacklistToken(String token) {
        try {
            String jti = jwtTokenProvider.getJti(token);
            Date expiration = jwtTokenProvider.getExpirationDate(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().set("sf:token:blacklist:" + jti, "1", Duration.ofMillis(ttl));
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    private String generateToken(String userId, String tenantId, long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
