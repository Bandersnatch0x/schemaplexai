package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.service.SecurityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/security-policies")
@RequiredArgsConstructor
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSecurityPolicy securityPolicy) {
        securityPolicyService.save(securityPolicy);
        return Result.success(securityPolicy.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSecurityPolicy securityPolicy) {
        securityPolicy.setId(id);
        return Result.success(securityPolicyService.updateById(securityPolicy));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(securityPolicyService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSecurityPolicy> get(@PathVariable Long id) {
        SfSecurityPolicy policy = securityPolicyService.getById(id);
        if (policy == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(policy);
    }

    @GetMapping
    public Result<List<SfSecurityPolicy>> list() {
        return Result.success(securityPolicyService.list());
    }
}
