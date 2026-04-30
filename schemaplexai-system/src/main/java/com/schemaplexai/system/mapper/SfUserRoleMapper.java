package com.schemaplexai.system.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.system.entity.SfUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SfUserRoleMapper extends BaseMapperX<SfUserRole> {

    @Select("SELECT role_id FROM sf_user_role WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
