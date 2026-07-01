package com.schemaplexai.system.security;

import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.mapper.SfPermissionMapper;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorTest {

    @Mock
    private SfUserRoleMapper userRoleMapper;

    @Mock
    private SfRolePermissionMapper rolePermissionMapper;

    @Mock
    private SfRoleMapper roleMapper;

    @Mock
    private SfPermissionMapper permissionMapper;

    @InjectMocks
    private PermissionEvaluator permissionEvaluator;

    @Test
    void hasPermission_userHasPermission_returnsTrue() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of(100L));
        when(permissionMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(createPermission(100L, "user:read")));

        boolean result = permissionEvaluator.hasPermission(1L, "user:read");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermission_userLacksPermission_returnsFalse() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of(100L));
        when(permissionMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(createPermission(100L, "user:read")));

        boolean result = permissionEvaluator.hasPermission(1L, "user:write");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_userHasNoRoles_returnsFalse() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of());

        boolean result = permissionEvaluator.hasPermission(1L, "anything");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_roleHasNoPermissions_returnsFalse() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of());

        boolean result = permissionEvaluator.hasPermission(1L, "anything");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermission_permissionFromMultipleRoles_aggregated() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L, 20L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of(100L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(20L)).thenReturn(List.of(200L));
        when(permissionMapper.selectBatchIds(List.of(100L, 200L)))
                .thenReturn(List.of(createPermission(100L, "user:read"), createPermission(200L, "admin:access")));

        assertThat(permissionEvaluator.hasPermission(1L, "user:read")).isTrue();
        assertThat(permissionEvaluator.hasPermission(1L, "admin:access")).isTrue();
        assertThat(permissionEvaluator.hasPermission(1L, "user:write")).isFalse();

        // Each hasPermission call triggers a resolvePermissionCodes call
        verify(permissionMapper, times(3)).selectBatchIds(List.of(100L, 200L));
    }

    @Test
    void hasRole_userHasRole_returnsTrue() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(createRole(10L, "admin")));

        boolean result = permissionEvaluator.hasRole(1L, "admin");

        assertThat(result).isTrue();
    }

    @Test
    void hasRole_userLacksRole_returnsFalse() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(roleMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(createRole(10L, "user")));

        boolean result = permissionEvaluator.hasRole(1L, "admin");

        assertThat(result).isFalse();
    }

    @Test
    void hasRole_userHasNoRoles_returnsFalse() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of());

        boolean result = permissionEvaluator.hasRole(1L, "anything");

        assertThat(result).isFalse();
    }

    @Test
    void resolvePermissionCodes_singleRole_returnsPermissionCodes() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of(100L));
        when(permissionMapper.selectBatchIds(List.of(100L)))
                .thenReturn(List.of(createPermission(100L, "perm:a")));

        Set<String> result = permissionEvaluator.resolvePermissionCodes(1L);

        assertThat(result).containsExactly("perm:a");
    }

    @Test
    void resolvePermissionCodes_multipleRoles_aggregatesAndBatchQueries() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L, 20L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(10L)).thenReturn(List.of(100L));
        when(rolePermissionMapper.selectPermissionIdsByRoleId(20L)).thenReturn(List.of(200L, 300L));
        when(permissionMapper.selectBatchIds(List.of(100L, 200L, 300L)))
                .thenReturn(List.of(
                        createPermission(100L, "perm:a"),
                        createPermission(200L, "perm:b"),
                        createPermission(300L, "perm:c")
                ));

        Set<String> result = permissionEvaluator.resolvePermissionCodes(1L);

        assertThat(result).containsExactlyInAnyOrder("perm:a", "perm:b", "perm:c");
        // Verify single batch call
        verify(permissionMapper, times(1)).selectBatchIds(any());
    }

    @Test
    void resolveRoleCodes_returnsAllRoleCodes() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(10L, 20L));
        when(roleMapper.selectBatchIds(List.of(10L, 20L)))
                .thenReturn(List.of(createRole(10L, "admin"), createRole(20L, "user")));

        Set<String> result = permissionEvaluator.resolveRoleCodes(1L);

        assertThat(result).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void resolveRoleCodes_noRoles_returnsEmptySet() {
        when(userRoleMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of());

        Set<String> result = permissionEvaluator.resolveRoleCodes(1L);

        assertThat(result).isEmpty();
    }

    // ==================== Helpers ====================

    private SfPermission createPermission(Long id, String code) {
        SfPermission p = new SfPermission();
        p.setId(id);
        p.setCode(code);
        p.setName(code);
        return p;
    }

    private SfRole createRole(Long id, String code) {
        SfRole r = new SfRole();
        r.setId(id);
        r.setCode(code);
        r.setName(code);
        return r;
    }
}
