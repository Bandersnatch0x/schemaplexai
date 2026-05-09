package com.schemaplexai.system.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.system.entity.TenantPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TenantPolicyMapper extends BaseMapperX<TenantPolicy> {

    @Select("SELECT * FROM sf_tenant_policy WHERE tenant_id = #{tenantId} AND deleted = 0")
    List<TenantPolicy> selectByTenantId(@Param("tenantId") String tenantId);
}
