package com.schemaplexai.system.service;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long jwtRefreshExpiration;

    @PostConstruct
    public void validateDependencies() {
        log.debug("AuthService initialized with jwtExpiration={}, jwtRefreshExpiration={}",
                jwtExpiration, jwtRefreshExpiration);
    }

    public Map<String, String> login(String username, String password, String tenantId) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "username or password is empty");
        }
        if (!StringUtils.hasText(tenantId)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenant id is empty");
        }

        SfUser user = userService.getByUsernameAndTenantId(username, tenantId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BaseException(ResultCode.PASSWORD_ERROR);
        }

        // Delegate to JwtTokenProvider which includes the username claim
        String accessToken = jwtTokenProvider.generateToken(
                user.getId().toString(), tenantId, user.getUsername());
        String refreshToken = generateRefreshToken(user.getId().toString(), tenantId);

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
            io.jsonwebtoken.Claims claims = jwtTokenProvider.parseToken(refreshToken);

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);
            String username = claims.get("username", String.class);

            if (username == null) {
                username = "unknown";
            }

            String newAccessToken = jwtTokenProvider.generateToken(userId, tenantId, username);
            String newRefreshToken = generateRefreshToken(userId, tenantId);

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

    /**
     * Changes the password for the given user. Validates the old password
     * before applying the new one.
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SfUser user = userService.getById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BaseException(ResultCode.PASSWORD_ERROR, "old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to update password");
        }

        // Invalidate existing sessions — force re-login after password change
        stringRedisTemplate.delete(TenantRedisKeyResolver.tokenSession(user.getId().toString()));
        log.info("Password changed for userId={}", userId);
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

    /**
     * Generates a refresh token. Delegates to JwtTokenProvider for the
     * actual JWT construction, using the longer refresh expiration.
     */
    private String generateRefreshToken(String userId, String tenantId) {
        return jwtTokenProvider.generateToken(userId, tenantId, jwtRefreshExpiration);
    }
}
