package com.schemaplexai.admin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.admin.dto.RoleAdminDTO;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.entity.SfRolePermission;
import com.schemaplexai.system.entity.SfUserRole;
import com.schemaplexai.system.mapper.SfPermissionMapper;
import com.schemaplexai.system.mapper.SfRoleMapper;
import com.schemaplexai.system.mapper.SfRolePermissionMapper;
import com.schemaplexai.system.mapper.SfUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAdminService extends ServiceImpl<SfRoleMapper, SfRole> {

    private final SfPermissionMapper permissionMapper;
    private final SfRolePermissionMapper rolePermissionMapper;
    private final SfUserRoleMapper userRoleMapper;

    public List<RoleAdminDTO> listAllRoles(String tenantId) {
        List<SfRole> roles = lambdaQuery()
                .eq(tenantId != null && !tenantId.isBlank(), SfRole::getTenantId, tenantId)
                .list();

        return roles.stream()
                .map(this::toRoleAdminDTO)
                .toList();
    }

    public RoleAdminDTO getRoleDetail(Long roleId) {
        SfRole role = getById(roleId);
        if (role == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "role not found");
        }
        return toRoleAdminDTO(role);
    }

    @Transactional
    public RoleAdminDTO createRole(String tenantId, String name, String code) {
        SfRole existing = lambdaQuery()
                .eq(SfRole::getCode, code)
                .eq(SfRole::getTenantId, tenantId)
                .one();
        if (existing != null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "role code already exists");
        }

        SfRole role = new SfRole();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setCode(code);
        save(role);

        log.info("Role created: id={}, code={}, tenantId={}", role.getId(), code, tenantId);
        return toRoleAdminDTO(role);
    }

    @Transactional
    public void updateRole(Long roleId, String name) {
        SfRole role = getById(roleId);
        if (role == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "role not found");
        }
        role.setName(name);
        updateById(role);
        log.info("Role updated: id={}, name={}", roleId, name);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        SfRole role = getById(roleId);
        if (role == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "role not found");
        }

        Long userCount = userRoleMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUserRole>()
                        .eq(SfUserRole::getRoleId, roleId)
        );
        if (userCount > 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "cannot delete role with assigned users");
        }

        rolePermissionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfRolePermission>()
                        .eq(SfRolePermission::getRoleId, roleId)
        );

        removeById(roleId);
        log.info("Role deleted: id={}, code={}", roleId, role.getCode());
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        SfRole role = getById(roleId);
        if (role == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "role not found");
        }

        rolePermissionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfRolePermission>()
                        .eq(SfRolePermission::getRoleId, roleId)
        );

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                SfRolePermission rp = new SfRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }

        log.info("Permissions assigned to role: roleId={}, count={}", roleId,
                permissionIds != null ? permissionIds.size() : 0);
    }

    public List<String> getRolePermissions(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfRolePermission>()
                        .eq(SfRolePermission::getRoleId, roleId)
        ).stream().map(SfRolePermission::getPermissionId).toList();

        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(SfPermission::getName)
                .toList();
    }

    private Long countUsersByRole(Long roleId) {
        return userRoleMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUserRole>()
                        .eq(SfUserRole::getRoleId, roleId)
        );
    }

    private RoleAdminDTO toRoleAdminDTO(SfRole role) {
        RoleAdminDTO dto = new RoleAdminDTO();
        dto.setId(role.getId());
        dto.setTenantId(role.getTenantId());
        dto.setName(role.getName());
        dto.setCode(role.getCode());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        dto.setPermissions(getRolePermissions(role.getId()));
        dto.setUserCount(countUsersByRole(role.getId()));
        return dto;
    }
}
