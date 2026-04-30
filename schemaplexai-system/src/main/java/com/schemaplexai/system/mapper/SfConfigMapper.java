package com.schemaplexai.system.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.system.entity.SfConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SfConfigMapper extends BaseMapperX<SfConfig> {

    @Select("SELECT * FROM sf_config WHERE config_key = #{configKey} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SfConfig selectByKeyAndTenantId(@Param("configKey") String configKey, @Param("tenantId") String tenantId);
}
