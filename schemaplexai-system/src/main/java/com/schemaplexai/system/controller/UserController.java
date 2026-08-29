package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.dto.UserCreateRequest;
import com.schemaplexai.system.dto.UserUpdateRequest;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.service.UserService;
import com.schemaplexai.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * REST controller for user management.
 * <p>
 * Deliberately returns {@link UserVO} instead of {@link SfUser} to prevent
 * password hash exposure via serialization.
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<UserVO>> page(PageParam pageParam) {
        var page = userService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        var records = page.getRecords().stream()
                .map(UserController::toVO)
                .collect(Collectors.toList());
        return Result.success(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        SfUser user = userService.getById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(toVO(user));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        SfUser user = new SfUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setTenantId(request.getTenantId());

        Long id = userService.register(user);
        return Result.success(id);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        SfUser existing = userService.getById(id);
        if (existing == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }

        SfUser update = new SfUser();
        update.setId(id);
        update.setUsername(request.getUsername());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setStatus(request.getStatus());
        return Result.success(userService.updateById(update));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }

    /**
     * Converts a {@link SfUser} entity to a {@link UserVO}, omitting the password hash.
     */
    static UserVO toVO(SfUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setTenantId(user.getTenantId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
