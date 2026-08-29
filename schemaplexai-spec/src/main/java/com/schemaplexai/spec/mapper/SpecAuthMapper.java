package com.schemaplexai.spec.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Read-only RBAC lookups against the shared governance tables (sf_role /
 * sf_user_role / sf_permission / sf_role_permission). The spec service
 * resolves the caller's role and permission codes itself because the gateway
 * only forwards identity claims (userId / tenantId) — never roles.
 * <p>
 * Tenant isolation is enforced twice: the MyBatis-Plus
 * {@code TenantLineInnerInterceptor} appends the tenant condition to these
 * statements, and the join keys themselves are tenant-scoped rows.
 */
public interface SpecAuthMapper {

    /**
     * Active (non-deleted) role codes assigned to the user.
     */
    @Select("SELECT r.code FROM sf_role r "
            + "JOIN sf_user_role ur ON ur.role_id = r.id AND ur.user_id = #{userId} "
            + "WHERE r.deleted = 0")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /**
     * Distinct permission codes granted to the user through their roles.
     * Permission codes that follow the {@code spec:*} authority convention
     * (e.g. {@code spec:publish}) are passed through as authorities verbatim.
     */
    @Select("SELECT DISTINCT p.code FROM sf_permission p "
            + "JOIN sf_role_permission rp ON rp.permission_id = p.id "
            + "JOIN sf_user_role ur ON ur.role_id = rp.role_id AND ur.user_id = #{userId} "
            + "WHERE p.deleted = 0")
    List<String> selectPermissionCodes(@Param("userId") Long userId);
}
