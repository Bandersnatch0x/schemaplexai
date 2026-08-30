package com.schemaplexai.system.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.mapper.SfUserMapper;
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

    private UserService userService;

    private SfUser sampleUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(passwordEncoder);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        sampleUser = new SfUser();
        sampleUser.setId(100L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("$2a$10$encodedPassword");
        sampleUser.setEmail("test@example.com");
        sampleUser.setTenantId("tenant-1");
        sampleUser.setStatus("ACTIVE");
    }

    @Test
    void getByUsernameAndTenantId_returnsUser() {
        when(userMapper.selectByUsernameAndTenantId("testuser", 7L))
                .thenReturn(sampleUser);

        SfUser result = userService.getByUsernameAndTenantId("testuser", "7");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getByUsernameAndTenantId_returnsNullWhenNotFound() {
        when(userMapper.selectByUsernameAndTenantId("nonexistent", 7L))
                .thenReturn(null);

        SfUser result = userService.getByUsernameAndTenantId("nonexistent", "7");

        assertThat(result).isNull();
    }

    @Test
    void getByUsernameAndTenantId_nonNumericTenant_returnsNullWithoutQuery() {
        // Live defect regression: tenant_id is BIGINT; a non-numeric tenant id
        // must not reach the database as a varchar parameter.
        SfUser result = userService.getByUsernameAndTenantId("testuser", "tenant-x");

        assertThat(result).isNull();
        org.mockito.Mockito.verify(userMapper, org.mockito.Mockito.never())
                .selectByUsernameAndTenantId(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong());
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
        assertThat(newUser.getStatus()).isEqualTo("ACTIVE");
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
    void register_whenSaveAffectsNoRows_throwsInternalError() {
        SfUser newUser = new SfUser();
        newUser.setUsername("newuser");
        newUser.setPassword("rawPassword");
        newUser.setEmail("new@example.com");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encodedNew");
        when(userMapper.insert(any(SfUser.class))).thenReturn(0);

        assertThatThrownBy(() -> userService.register(newUser))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }

    @Test
    void register_preservesExplicitStatus() {
        SfUser newUser = new SfUser();
        newUser.setUsername("statususer");
        newUser.setPassword("rawPassword");
        newUser.setStatus("DISABLED");

        when(userMapper.selectByUsername("statususer")).thenReturn(null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(SfUser.class))).thenAnswer(invocation -> {
            SfUser saved = invocation.getArgument(0);
            saved.setId(300L);
            return 1;
        });

        userService.register(newUser);

        assertThat(newUser.getStatus()).isEqualTo("DISABLED");
    }
}
