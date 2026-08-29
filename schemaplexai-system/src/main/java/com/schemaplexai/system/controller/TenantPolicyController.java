package com.schemaplexai.system.controller;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.dto.TenantPolicyRequest;
import com.schemaplexai.system.entity.TenantPolicy;
import com.schemaplexai.system.service.TenantPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for tenant policy CRUD operations.
 * Policies are scoped to the current tenant from the request header.
 */
@Tag(name = "租户策略管理")
@RestController
@RequestMapping("/system/tenant-policies")
@RequiredArgsConstructor
public class TenantPolicyController {

    private final TenantPolicyService tenantPolicyService;

    @Operation(summary = "查询当前租户的所有策略")
    @GetMapping
    public Result<List<TenantPolicy>> listPolicies(HttpServletRequest request) {
        String tenantId = resolveTenantId(request);
        List<TenantPolicy> policies = tenantPolicyService.getPoliciesByTenant(tenantId);
        return Result.success(policies);
    }

    @Operation(summary = "查询当前租户的指定策略")
    @GetMapping("/{policyType}")
    public Result<TenantPolicy> getPolicy(@PathVariable String policyType, HttpServletRequest request) {
        if (!StringUtils.hasText(policyType)) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "policyType must not be blank");
        }
        String tenantId = resolveTenantId(request);
        TenantPolicy policy = tenantPolicyService.getPolicy(tenantId, policyType);
        if (policy == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(policy);
    }

    @Operation(summary = "创建或更新租户策略")
    @PutMapping("/{policyType}")
    public Result<Void> saveOrUpdatePolicy(@PathVariable String policyType,
                                           @Valid @RequestBody TenantPolicyRequest body,
                                           HttpServletRequest request) {
        String tenantId = resolveTenantId(request);
        tenantPolicyService.saveOrUpdatePolicy(tenantId, policyType, body.getConfigJson());
        return Result.success();
    }

    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        if (!StringUtils.hasText(tenantId)) {
            throw new com.schemaplexai.common.exception.BaseException(
                    ResultCode.PARAM_ERROR, "X-Tenant-Id header is required");
        }
        return tenantId;
    }
}
