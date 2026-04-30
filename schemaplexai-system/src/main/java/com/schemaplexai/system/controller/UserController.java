package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    public Result<PageResult<SfUser>> page(PageParam pageParam) {
        var page = userService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<SfUser> getById(@PathVariable Long id) {
        SfUser user = userService.getById(id);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(user);
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<Long> create(@RequestBody SfUser user) {
        userService.save(user);
        return Result.success(user.getId());
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfUser user) {
        user.setId(id);
        return Result.success(userService.updateById(user));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }
}
