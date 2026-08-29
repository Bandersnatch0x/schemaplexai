package com.schemaplexai.system.security;

import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.mapper.SfPermissionMapper;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluates whether a user has a specific permission or role.
 * <p>
 * Loads role and permission IDs from the link tables, then resolves
 * the code from the role/permission entity tables. Results are not
 * cached here; caching is handled by {@link RbacService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final SfUserRoleMapper userRoleMapper;
    private final SfRolePermissionMapper rolePermissionMapper;
    private final SfRoleMapper roleMapper;
    private final SfPermissionMapper permissionMapper;

    /**
     * Returns true if the given user has the specified permission code.
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        Set<String> permissions = resolvePermissionCodes(userId);
        boolean has = permissions.contains(permissionCode);
        if (!has) {
            log.debug("Permission denied: userId={}, required={}, actual={}", userId, permissionCode, permissions);
        }
        return has;
    }

    /**
     * Returns true if the given user has the specified role code.
     */
    public boolean hasRole(Long userId, String roleCode) {
        Set<String> roles = resolveRoleCodes(userId);
        boolean has = roles.contains(roleCode);
        if (!has) {
            log.debug("Role denied: userId={}, required={}, actual={}", userId, roleCode, roles);
        }
        return has;
    }

    /**
     * Resolves all permission codes for a user by traversing user->role->permission.
     * <p>
     * Collects all permission IDs first, then performs a single batch query
     * to avoid N+1 database calls.
     */
    public Set<String> resolvePermissionCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        // Collect all permission IDs across all roles
        List<Long> allPermissionIds = roleIds.stream()
                .flatMap(roleId -> rolePermissionMapper.selectPermissionIdsByRoleId(roleId).stream())
                .distinct()
                .toList();
        if (allPermissionIds.isEmpty()) {
            return Set.of();
        }
        // Single batch query
        return permissionMapper.selectBatchIds(allPermissionIds).stream()
                .map(SfPermission::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }

    /**
     * Resolves all role codes for a user.
     */
    public Set<String> resolveRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SfRole::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }
}
