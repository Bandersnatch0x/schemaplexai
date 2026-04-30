package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.mapper.SfUserMapper;
import com.schemaplexai.system.security.JwtTokenProvider;
import com.schemaplexai.system.user.dto.LoginRequest;
import com.schemaplexai.system.user.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SfUserMapper, SfUser> {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public SfUser getByUsernameAndTenantId(String username, String tenantId) {
        return baseMapper.selectByUsernameAndTenantId(username, tenantId);
    }

    public LoginResponse login(LoginRequest request) {
        SfUser user = baseMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BaseException(ResultCode.FORBIDDEN, "user disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(ResultCode.PASSWORD_ERROR);
        }

        String tenantId = user.getTenantId() != null ? user.getTenantId() : request.getTenantId();
        if (tenantId == null) {
            tenantId = "default";
        }

        String token = jwtTokenProvider.generateToken(
                String.valueOf(user.getId()),
                tenantId,
                user.getUsername()
        );

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(86400L);
        response.setUsername(user.getUsername());
        response.setTenantId(tenantId);
        return response;
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
        save(user);
        return user.getId();
    }

    public PageResult<SfUser> pageUsers(PageParam pageParam) {
        Page<SfUser> page = page(new Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
