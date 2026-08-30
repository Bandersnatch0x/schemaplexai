package com.schemaplexai.task.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only lookup of display names on {@code sf_user} for task comment authors.
 *
 * <p>{@code sf_user} is on the tenant-line interceptor's ignore list (login runs
 * before any tenant context exists), so this query scopes {@code tenant_id}
 * explicitly instead of relying on automatic injection.
 */
@Mapper
public interface SfUserLookupMapper {

    @Select("SELECT username FROM sf_user "
            + "WHERE id = #{userId} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    String findUsernameById(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
