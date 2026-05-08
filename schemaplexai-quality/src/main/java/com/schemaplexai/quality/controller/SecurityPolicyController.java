package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.service.SecurityPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/security-policies")
@RequiredArgsConstructor
@Tag(name = "安全策略管理")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @Operation(summary = "创建安全策略")
    @PostMapping
    public Result<Long> create(@RequestBody SfSecurityPolicy securityPolicy) {
        securityPolicyService.save(securityPolicy);
        return Result.success(securityPolicy.getId());
    }

    @Operation(summary = "更新安全策略")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSecurityPolicy securityPolicy) {
        securityPolicy.setId(id);
        return Result.success(securityPolicyService.updateById(securityPolicy));
    }

    @Operation(summary = "删除安全策略")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(securityPolicyService.removeById(id));
    }

    @Operation(summary = "获取安全策略详情")
    @GetMapping("/{id}")
    public Result<SfSecurityPolicy> get(@PathVariable Long id) {
        SfSecurityPolicy policy = securityPolicyService.getById(id);
        if (policy == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(policy);
    }

    @Operation(summary = "获取安全策略列表")
    @GetMapping
    public Result<List<SfSecurityPolicy>> list() {
        return Result.success(securityPolicyService.list());
    }
}
