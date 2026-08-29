package com.schemaplexai.system.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private SfUser sampleUser;
    private static final String SECRET = "a]B@cD3fG6hI9kL2mN5oP8rS1tU4vW7xY0zA3bC6dE9fG2hI5kL8mN1oP4rS7tU0vW";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(authService, "jwtRefreshExpiration", 604800000L);

        sampleUser = new SfUser();
        sampleUser.setId(100L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("$2a$10$encodedPassword");
        sampleUser.setTenantId("tenant-1");
        sampleUser.setStatus(1);
    }

    // ==================== Login ====================

    @Test
    void login_validCredentials_delegatesToJwtTokenProvider() {
        when(userService.getByUsernameAndTenantId("testuser", "tenant-1")).thenReturn(sampleUser);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        sampleUser.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("rawPassword"));
        when(jwtTokenProvider.generateToken("100", "tenant-1", "testuser")).thenReturn("access-token");
        when(jwtTokenProvider.generateToken("100", "tenant-1", 604800000L)).thenReturn("refresh-token");

        Map<String, String> result = authService.login("testuser", "rawPassword", "tenant-1");

        assertThat(result).containsKeys("accessToken", "refreshToken", "tokenType");
        assertThat(result.get("accessToken")).isEqualTo("access-token");
        assertThat(result.get("refreshToken")).isEqualTo("refresh-token");
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
        verify(jwtTokenProvider).generateToken("100", "tenant-1", "testuser");
    }

    @Test
    void login_emptyUsername_throwsParamError() {
        assertThatThrownBy(() -> authService.login("", "password", "tenant-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void login_nullPassword_throwsParamError() {
        assertThatThrownBy(() -> authService.login("testuser", null, "tenant-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void login_blankTenantId_throwsParamErrorWithoutUserLookup() {
        assertThatThrownBy(() -> authService.login("testuser", "password", " "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(userService, never()).getByUsernameAndTenantId(anyString(), anyString());
    }

    @Test
    void login_userNotFound_throwsUserNotFound() {
        when(userService.getByUsernameAndTenantId("nonexistent", "tenant-1")).thenReturn(null);

        assertThatThrownBy(() -> authService.login("nonexistent", "password", "tenant-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void login_wrongPassword_throwsPasswordError() {
        when(userService.getByUsernameAndTenantId("testuser", "tenant-1")).thenReturn(sampleUser);

        assertThatThrownBy(() -> authService.login("testuser", "wrongPassword", "tenant-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PASSWORD_ERROR.getCode());
    }

    // ==================== Refresh Token ====================

    @Test
    void refreshToken_validRefreshToken_returnsNewTokens() {
        // Create a real JWT that can be parsed by JwtTokenProvider
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String refreshToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("100")
                .claim("tenantId", "tenant-1")
                .claim("username", "testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 604800_000))
                .signWith(key)
                .compact();

        // jwtTokenProvider is mocked, but for parseToken we need a real result
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.generateToken("100", "tenant-1", "testuser")).thenReturn("new-access");
        when(jwtTokenProvider.generateToken("100", "tenant-1", 604800000L)).thenReturn("new-refresh");

        Map<String, String> result = authService.refreshToken(refreshToken);

        assertThat(result).containsKeys("accessToken", "refreshToken", "tokenType");
        assertThat(result.get("accessToken")).isEqualTo("new-access");
        assertThat(result.get("refreshToken")).isEqualTo("new-refresh");
        verify(jwtTokenProvider).generateToken("100", "tenant-1", "testuser");
    }

    @Test
    void refreshToken_emptyToken_throwsParamError() {
        assertThatThrownBy(() -> authService.refreshToken(""))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void refreshToken_invalidToken_throwsTokenInvalid() {
        when(jwtTokenProvider.parseToken("invalid.token.value")).thenThrow(new RuntimeException("bad token"));

        assertThatThrownBy(() -> authService.refreshToken("invalid.token.value"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOKEN_INVALID.getCode());
    }

    // ==================== Logout ====================

    @Test
    void logout_withUserId_deletesRedisKey() {
        authService.logout("100");

        verify(stringRedisTemplate).delete("sf:global:token:session:100");
    }

    @Test
    void logout_withNullUserId_doesNotDelete() {
        authService.logout(null);

        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void logout_withToken_blacklistsToken() {
        String token = createTokenWithExpiration(new Date(System.currentTimeMillis() + 3600_000));
        when(jwtTokenProvider.getJti(token)).thenReturn("test-jti");
        when(jwtTokenProvider.getExpirationDate(token)).thenReturn(new Date(System.currentTimeMillis() + 3600_000));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout("100", token);

        verify(valueOps).set(eq("sf:token:blacklist:test-jti"), eq("1"), any(Duration.class));
    }

    // ==================== Token Blacklist ====================

    @Test
    void isTokenBlacklisted_blacklistedToken_returnsTrue() {
        String token = createTokenWithExpiration(new Date(System.currentTimeMillis() + 3600_000));
        when(jwtTokenProvider.getJti(token)).thenReturn("test-jti");
        when(stringRedisTemplate.hasKey("sf:token:blacklist:test-jti")).thenReturn(true);

        boolean result = authService.isTokenBlacklisted(token);

        assertThat(result).isTrue();
    }

    @Test
    void isTokenBlacklisted_nonBlacklistedToken_returnsFalse() {
        String token = createTokenWithExpiration(new Date(System.currentTimeMillis() + 3600_000));
        when(jwtTokenProvider.getJti(token)).thenReturn("test-jti-2");
        when(stringRedisTemplate.hasKey("sf:token:blacklist:test-jti-2")).thenReturn(false);

        boolean result = authService.isTokenBlacklisted(token);

        assertThat(result).isFalse();
    }

    @Test
    void blacklistToken_validToken_setsBlacklistEntry() {
        Date expiration = new Date(System.currentTimeMillis() + 3600_000);
        String token = createTokenWithExpiration(expiration);
        when(jwtTokenProvider.getJti(token)).thenReturn("blacklist-jti");
        when(jwtTokenProvider.getExpirationDate(token)).thenReturn(expiration);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        authService.blacklistToken(token);

        verify(valueOps).set(eq("sf:token:blacklist:blacklist-jti"), eq("1"), any(Duration.class));
    }

    // ==================== Change Password ====================

    @Test
    void changePassword_validRequest_updatesPasswordAndInvalidatesSession() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("oldPass"));

        when(userService.getById(1L)).thenReturn(user);
        when(userService.updateById(any())).thenReturn(true);

        authService.changePassword(1L, "oldPass", "newPass");

        verify(userService).updateById(argThat(u ->
                u.getId().equals(1L) && !u.getPassword().equals("$2a$10$encodedPassword")
        ));
        verify(stringRedisTemplate).delete("sf:global:token:session:1");
    }

    @Test
    void changePassword_userNotFound_throwsUserNotFound() {
        when(userService.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> authService.changePassword(99L, "old", "new"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void changePassword_wrongOldPassword_throwsPasswordError() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correctOld"));

        when(userService.getById(1L)).thenReturn(user);

        assertThatThrownBy(() -> authService.changePassword(1L, "wrongOld", "newPass"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PASSWORD_ERROR.getCode());
    }

    @Test
    void changePassword_updateFails_throwsInternalError() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("oldPass"));

        when(userService.getById(1L)).thenReturn(user);
        when(userService.updateById(any())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, "oldPass", "newPass"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }

    // ==================== Helpers ====================

    private String createTokenWithExpiration(Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("100")
                .claim("tenantId", "tenant-1")
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
