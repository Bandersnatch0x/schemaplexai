package com.schemaplexai.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.admin.dto.UserAdminDTO;
import com.schemaplexai.admin.dto.UserAdminQuery;
import com.schemaplexai.admin.dto.UserRoleUpdateDTO;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.entity.SfUserRole;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfUserMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final SfUserMapper userMapper;
    private final SfUserRoleMapper userRoleMapper;
    private final SfRoleMapper roleMapper;
    private final SfAuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserAdminDTO> queryUsers(UserAdminQuery query) {
        Page<SfUser> page = userMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUser>()
                        .eq(query.getTenantId() != null && !query.getTenantId().isBlank(), SfUser::getTenantId, query.getTenantId())
                        .like(query.getUsername() != null && !query.getUsername().isBlank(), SfUser::getUsername, query.getUsername())
                        .like(query.getEmail() != null && !query.getEmail().isBlank(), SfUser::getEmail, query.getEmail())
                        .like(query.getPhone() != null && !query.getPhone().isBlank(), SfUser::getPhone, query.getPhone())
                        .eq(query.getStatus() != null, SfUser::getStatus, query.getStatus())
                        .orderByDesc(SfUser::getCreatedAt)
        );

        List<UserAdminDTO> dtos = page.getRecords().stream()
                .map(this::toUserAdminDTO)
                .toList();

        return PageResult.of(dtos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public UserAdminDTO getUserDetail(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }
        return toUserAdminDTO(user);
    }

    @Transactional
    public void disableUser(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("User disabled: id={}, username={}", userId, user.getUsername());
    }

    @Transactional
    public void enableUser(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        userMapper.updateById(user);
        log.info("User enabled: id={}, username={}", userId, user.getUsername());
    }

    @Transactional
    public String resetUserPassword(Long userId) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }
        String rawPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        log.info("Password reset for user: id={}, username={}", userId, user.getUsername());
        return rawPassword;
    }

    @Transactional
    public void updateUserRoles(Long userId, UserRoleUpdateDTO dto) {
        SfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ResultCode.USER_NOT_FOUND);
        }

        userRoleMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUserRole>()
                        .eq(SfUserRole::getUserId, userId)
        );

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                SfUserRole userRole = new SfUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        log.info("User roles updated: userId={}, roleCount={}", userId,
                dto.getRoleIds() != null ? dto.getRoleIds().size() : 0);
    }

    public List<String> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SfRole::getName)
                .toList();
    }

    private UserAdminDTO toUserAdminDTO(SfUser user) {
        UserAdminDTO dto = new UserAdminDTO();
        dto.setId(user.getId());
        dto.setTenantId(user.getTenantId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoles(getUserRoles(user.getId()));
        dto.setAuditLogCount(countAuditLogsByUser(user.getId()));
        dto.setLastLoginAt(getLastLoginAt(user.getId()));
        return dto;
    }

    private Long countAuditLogsByUser(Long userId) {
        return auditLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAuditLog>()
                        .eq(SfAuditLog::getUserId, userId)
        );
    }

    private LocalDateTime getLastLoginAt(Long userId) {
        List<SfAuditLog> logs = auditLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAuditLog>()
                        .eq(SfAuditLog::getUserId, userId)
                        .eq(SfAuditLog::getAction, "LOGIN")
                        .eq(SfAuditLog::getStatus, 1)
                        .orderByDesc(SfAuditLog::getExecutedAt)
                        .last("LIMIT 1")
        );
        return logs.isEmpty() ? null : logs.get(0).getExecutedAt();
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }
}
