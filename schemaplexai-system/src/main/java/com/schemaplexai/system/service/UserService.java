package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SfUserMapper, SfUser> {

    private final PasswordEncoder passwordEncoder;

    public SfUser getByUsernameAndTenantId(String username, String tenantId) {
        return baseMapper.selectByUsernameAndTenantId(username, tenantId);
    }

    public Long register(SfUser user) {
        SfUser exist = baseMapper.selectByUsername(user.getUsername());
        if (exist != null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        boolean saved = save(user);
        if (!saved) {
            throw new BaseException(ResultCode.INTERNAL_ERROR);
        }
        return user.getId();
    }

    public PageResult<SfUser> pageUsers(PageParam pageParam) {
        Page<SfUser> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        page = page(page);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
