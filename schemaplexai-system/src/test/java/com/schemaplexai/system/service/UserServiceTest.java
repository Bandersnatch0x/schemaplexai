package com.schemaplexai.system.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.mapper.SfUserMapper;
import com.schemaplexai.system.security.JwtTokenProvider;
import com.schemaplexai.system.user.dto.LoginRequest;
import com.schemaplexai.system.user.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SfUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserService userService;

    private SfUser sampleUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(passwordEncoder, jwtTokenProvider);
        // ServiceImpl stores the mapper in the baseMapper field
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        sampleUser = new SfUser();
        sampleUser.setId(100L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("$2a$10$encodedPassword");
        sampleUser.setEmail("test@example.com");
        sampleUser.setTenantId("tenant-1");
        sampleUser.setStatus(1);
    }

    @Test
    void getByUsernameAndTenantId_returnsUser() {
        when(userMapper.selectByUsernameAndTenantId("testuser", "tenant-1"))
                .thenReturn(sampleUser);

        SfUser result = userService.getByUsernameAndTenantId("testuser", "tenant-1");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void getByUsernameAndTenantId_returnsNullWhenNotFound() {
        when(userMapper.selectByUsernameAndTenantId("nonexistent", "tenant-1"))
                .thenReturn(null);

        SfUser result = userService.getByUsernameAndTenantId("nonexistent", "tenant-1");

        assertThat(result).isNull();
    }

    @Test
    void login_validCredentials_returnsLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("rawPassword");
        request.setTenantId("tenant-1");

        when(userMapper.selectByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken("100", "tenant-1", "testuser")).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getTenantId()).isEqualTo("tenant-1");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
    }

    @Test
    void login_userNotFound_throwsUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");
        request.setTenantId("tenant-1");

        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void login_disabledUser_throwsForbidden() {
        sampleUser.setStatus(0);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("rawPassword");
        request.setTenantId("tenant-1");

        when(userMapper.selectByUsername("testuser")).thenReturn(sampleUser);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void login_wrongPassword_throwsPasswordError() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");
        request.setTenantId("tenant-1");

        when(userMapper.selectByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PASSWORD_ERROR.getCode());
    }

    @Test
    void login_nullTenantIdInUser_fallsBackToRequestTenantId() {
        sampleUser.setTenantId(null);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("rawPassword");
        request.setTenantId("request-tenant");

        when(userMapper.selectByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken("100", "request-tenant", "testuser")).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertThat(response.getTenantId()).isEqualTo("request-tenant");
    }

    @Test
    void login_nullTenantIdEverywhere_usesDefault() {
        sampleUser.setTenantId(null);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("rawPassword");
        request.setTenantId(null);

        when(userMapper.selectByUsername("testuser")).thenReturn(sampleUser);
        when(passwordEncoder.matches("rawPassword", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken("100", "default", "testuser")).thenReturn("jwt-token");

        LoginResponse response = userService.login(request);

        assertThat(response.getTenantId()).isEqualTo("default");
    }

    @Test
    void register_newUser_savesAndReturnsId() {
        SfUser newUser = new SfUser();
        newUser.setUsername("newuser");
        newUser.setPassword("rawPassword");
        newUser.setEmail("new@example.com");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encodedNew");
        when(userMapper.insert(any(SfUser.class))).thenAnswer(invocation -> {
            SfUser saved = invocation.getArgument(0);
            saved.setId(200L);
            return 1;
        });

        Long id = userService.register(newUser);

        assertThat(id).isEqualTo(200L);
        assertThat(newUser.getPassword()).isEqualTo("$2a$10$encodedNew");
        assertThat(newUser.getStatus()).isEqualTo(1);
    }

    @Test
    void register_existingUsername_throwsParamError() {
        SfUser existingUser = new SfUser();
        existingUser.setId(100L);
        existingUser.setUsername("existinguser");

        SfUser newUser = new SfUser();
        newUser.setUsername("existinguser");
        newUser.setPassword("password");

        when(userMapper.selectByUsername("existinguser")).thenReturn(existingUser);

        assertThatThrownBy(() -> userService.register(newUser))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void register_preservesExplicitStatus() {
        SfUser newUser = new SfUser();
        newUser.setUsername("statususer");
        newUser.setPassword("rawPassword");
        newUser.setStatus(0);

        when(userMapper.selectByUsername("statususer")).thenReturn(null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(SfUser.class))).thenAnswer(invocation -> {
            SfUser saved = invocation.getArgument(0);
            saved.setId(300L);
            return 1;
        });

        userService.register(newUser);

        assertThat(newUser.getStatus()).isEqualTo(0);
    }
}
