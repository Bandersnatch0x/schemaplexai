package com.schemaplexai.system.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.system.entity.SfRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SfRolePermissionMapper extends BaseMapperX<SfRolePermission> {

    @Select("SELECT permission_id FROM sf_role_permission WHERE role_id = #{roleId} AND deleted = 0")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
