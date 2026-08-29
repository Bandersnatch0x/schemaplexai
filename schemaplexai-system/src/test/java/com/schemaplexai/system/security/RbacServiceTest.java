package com.schemaplexai.system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private PermissionEvaluator permissionEvaluator;

    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService(permissionEvaluator);
    }

    @Test
    void getUserPermissions_delegatesToEvaluatorOnFirstCall() {
        when(permissionEvaluator.resolvePermissionCodes(1L)).thenReturn(Set.of("user:read", "user:write"));

        Set<String> result = rbacService.getUserPermissions(1L);

        assertThat(result).containsExactlyInAnyOrder("user:read", "user:write");
        verify(permissionEvaluator, times(1)).resolvePermissionCodes(1L);
    }

    @Test
    void getUserPermissions_usesCacheOnSecondCall() {
        when(permissionEvaluator.resolvePermissionCodes(1L)).thenReturn(Set.of("user:read"));

        rbacService.getUserPermissions(1L);
        rbacService.getUserPermissions(1L);

        // Should only call the evaluator once — second call comes from cache
        verify(permissionEvaluator, times(1)).resolvePermissionCodes(1L);
    }

    @Test
    void getUserRoles_delegatesToEvaluatorOnFirstCall() {
        when(permissionEvaluator.resolveRoleCodes(1L)).thenReturn(Set.of("admin"));

        Set<String> result = rbacService.getUserRoles(1L);

        assertThat(result).containsExactly("admin");
        verify(permissionEvaluator, times(1)).resolveRoleCodes(1L);
    }

    @Test
    void getUserRoles_usesCacheOnSecondCall() {
        when(permissionEvaluator.resolveRoleCodes(1L)).thenReturn(Set.of("admin"));

        rbacService.getUserRoles(1L);
        rbacService.getUserRoles(1L);

        verify(permissionEvaluator, times(1)).resolveRoleCodes(1L);
    }

    @Test
    void evictUser_clearsCacheForThatUser() {
        when(permissionEvaluator.resolvePermissionCodes(1L)).thenReturn(Set.of("perm:a"));
        when(permissionEvaluator.resolveRoleCodes(1L)).thenReturn(Set.of("admin"));

        // Populate cache
        rbacService.getUserPermissions(1L);
        rbacService.getUserRoles(1L);

        // Evict
        rbacService.evictUser(1L);

        // Calling again should hit the evaluator again
        rbacService.getUserPermissions(1L);
        rbacService.getUserRoles(1L);

        verify(permissionEvaluator, times(2)).resolvePermissionCodes(1L);
        verify(permissionEvaluator, times(2)).resolveRoleCodes(1L);
    }

    @Test
    void evictAll_clearsAllCaches() {
        when(permissionEvaluator.resolvePermissionCodes(1L)).thenReturn(Set.of("perm:a"));
        when(permissionEvaluator.resolvePermissionCodes(2L)).thenReturn(Set.of("perm:b"));
        when(permissionEvaluator.resolveRoleCodes(1L)).thenReturn(Set.of("admin"));

        // Populate cache for two users
        rbacService.getUserPermissions(1L);
        rbacService.getUserPermissions(2L);
        rbacService.getUserRoles(1L);

        // Evict all
        rbacService.evictAll();

        // Calling again should hit evaluator
        rbacService.getUserPermissions(1L);
        rbacService.getUserPermissions(2L);
        rbacService.getUserRoles(1L);

        verify(permissionEvaluator, times(2)).resolvePermissionCodes(1L);
        verify(permissionEvaluator, times(2)).resolvePermissionCodes(2L);
        verify(permissionEvaluator, times(2)).resolveRoleCodes(1L);
    }

    @Test
    void cacheIsolatedByUser() {
        when(permissionEvaluator.resolvePermissionCodes(1L)).thenReturn(Set.of("user:read"));
        when(permissionEvaluator.resolvePermissionCodes(2L)).thenReturn(Set.of("admin:access"));

        Set<String> user1Perms = rbacService.getUserPermissions(1L);
        Set<String> user2Perms = rbacService.getUserPermissions(2L);

        assertThat(user1Perms).containsExactly("user:read");
        assertThat(user2Perms).containsExactly("admin:access");
        verify(permissionEvaluator, times(1)).resolvePermissionCodes(1L);
        verify(permissionEvaluator, times(1)).resolvePermissionCodes(2L);
    }
}
