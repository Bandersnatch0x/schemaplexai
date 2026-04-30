package com.schemaplexai.system.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.system.entity.SfUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SfUserMapper extends BaseMapperX<SfUser> {

    @Select("SELECT * FROM sf_user WHERE username = #{username} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SfUser selectByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") String tenantId);

    @Select("SELECT * FROM sf_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SfUser selectByUsername(@Param("username") String username);
}
