package com.schemaplexai.admin.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.entity.SfUserRole;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfUserMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private SfUserMapper userMapper;

    @Mock
    private SfUserRoleMapper userRoleMapper;

    @Mock
    private SfRoleMapper roleMapper;

    @Mock
    private com.schemaplexai.admin.mapper.SfAuditLogMapper auditLogMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAdminService userAdminService;

    // ------------------------------------------------------------------
    // getUserDetail
    // ------------------------------------------------------------------

    @Test
    void getUserDetail_notFound_throwsUserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userAdminService.getUserDetail(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void getUserDetail_success_returnsDto() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setUsername("alice");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(Collections.emptyList());

        var result = userAdminService.getUserDetail(1L);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    // ------------------------------------------------------------------
    // disableUser
    // ------------------------------------------------------------------

    @Test
    void disableUser_notFound_throwsUserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userAdminService.disableUser(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void disableUser_success_setsStatusZero() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userAdminService.disableUser(1L);

        assertThat(user.getStatus()).isEqualTo(0);
        verify(userMapper).updateById(user);
    }

    // ------------------------------------------------------------------
    // enableUser
    // ------------------------------------------------------------------

    @Test
    void enableUser_notFound_throwsUserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userAdminService.enableUser(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void enableUser_success_setsStatusOne() {
        SfUser user = new SfUser();
        user.setId(1L);
        user.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userAdminService.enableUser(1L);

        assertThat(user.getStatus()).isEqualTo(1);
        verify(userMapper).updateById(user);
    }

    // ------------------------------------------------------------------
    // resetUserPassword
    // ------------------------------------------------------------------

    @Test
    void resetUserPassword_notFound_throwsUserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userAdminService.resetUserPassword(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void resetUserPassword_success_returnsRawPassword() {
        SfUser user = new SfUser();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        String result = userAdminService.resetUserPassword(1L);

        assertThat(result).isNotBlank();
        assertThat(user.getPassword()).isEqualTo("encoded");
        verify(userMapper).updateById(user);
    }

    // ------------------------------------------------------------------
    // updateUserRoles
    // ------------------------------------------------------------------

    @Test
    void updateUserRoles_notFound_throwsUserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        var dto = new com.schemaplexai.admin.dto.UserRoleUpdateDTO();
        assertThatThrownBy(() -> userAdminService.updateUserRoles(1L, dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void updateUserRoles_success_replacesRoles() {
        SfUser user = new SfUser();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        var dto = new com.schemaplexai.admin.dto.UserRoleUpdateDTO();
        dto.setRoleIds(List.of(10L, 20L));

        userAdminService.updateUserRoles(1L, dto);

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper, times(2)).insert(any(SfUserRole.class));
    }

    @Test
    void updateUserRoles_nullRoleIds_deletesOnly() {
        SfUser user = new SfUser();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        var dto = new com.schemaplexai.admin.dto.UserRoleUpdateDTO();

        userAdminService.updateUserRoles(1L, dto);

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper, never()).insert(any(SfUserRole.class));
    }

    // ------------------------------------------------------------------
    // getUserRoles
    // ------------------------------------------------------------------

    @Test
    void getUserRoles_noRoles_returnsEmpty() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(Collections.emptyList());

        List<String> result = userAdminService.getUserRoles(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserRoles_withRoles_returnsRoleNames() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        SfRole role = new SfRole();
        role.setName("ADMIN");
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(role));

        List<String> result = userAdminService.getUserRoles(1L);

        assertThat(result).containsExactly("ADMIN");
    }
}
