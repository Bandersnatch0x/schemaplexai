package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.mapper.SfTenantMapper;
import org.springframework.stereotype.Service;

@Service
public class TenantService extends ServiceImpl<SfTenantMapper, SfTenant> {

    public SfTenant getByCode(String code) {
        return lambdaQuery().eq(SfTenant::getCode, code).one();
    }

    public SfTenant getValidTenant(Long id) {
        SfTenant tenant = getById(id);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() != null && tenant.getStatus() == 0) {
            throw new BaseException(ResultCode.TENANT_DISABLED);
        }
        return tenant;
    }
}
