package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.mapper.SecurityPolicyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SecurityPolicyServiceImpl extends ServiceImpl<SecurityPolicyMapper, SfSecurityPolicy> implements SecurityPolicyService {

    private final ObjectMapper objectMapper;

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DEPRECATED = 2;

    /**
     * Create a new security policy with validation.
     */
    @Override
    public boolean save(SfSecurityPolicy policy) {
        validatePolicy(policy);
        if (policy.getStatus() == null) {
            policy.setStatus(STATUS_DRAFT);
        }
        log.info("Creating security policy: name={}, type={}", policy.getName(), policy.getPolicyType());
        return super.save(policy);
    }

    /**
     * Update a security policy with validation.
     */
    @Override
    public boolean updateById(SfSecurityPolicy policy) {
        if (policy.getId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Policy ID is required for update");
        }
        SfSecurityPolicy existing = baseMapper.selectById(policy.getId());
        if (existing == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Security policy not found: " + policy.getId());
        }
        if (existing.getStatus() != null && existing.getStatus() == STATUS_DEPRECATED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot modify a deprecated policy");
        }
        if (policy.getName() != null) {
            validatePolicy(policy);
        }
        log.info("Updating security policy: id={}", policy.getId());
        return super.updateById(policy);
    }

    /**
     * Activate a security policy, making it enforceable.
     */
    public void activatePolicy(Long policyId) {
        SfSecurityPolicy policy = baseMapper.selectById(policyId);
        if (policy == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Security policy not found: " + policyId);
        }
        if (policy.getStatus() != null && policy.getStatus() == STATUS_DEPRECATED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Cannot activate a deprecated policy");
        }
        validateRulesJson(policy.getRulesJson());
        policy.setStatus(STATUS_ACTIVE);
        baseMapper.updateById(policy);
        log.info("Activated security policy: id={}", policyId);
    }

    /**
     * Deprecate a security policy. Deprecated policies cannot be reactivated.
     */
    public void deprecatePolicy(Long policyId) {
        SfSecurityPolicy policy = baseMapper.selectById(policyId);
        if (policy == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Security policy not found: " + policyId);
        }
        policy.setStatus(STATUS_DEPRECATED);
        baseMapper.updateById(policy);
        log.info("Deprecated security policy: id={}", policyId);
    }

    /**
     * Validate a resource against all active policies of a given type.
     * Returns a list of violation messages; empty list means compliant.
     */
    public List<String> validateAgainstPolicies(String policyType, Map<String, Object> resourceAttributes) {
        List<SfSecurityPolicy> policies = baseMapper.selectList(
            new LambdaQueryWrapper<SfSecurityPolicy>()
                .eq(SfSecurityPolicy::getPolicyType, policyType)
                .eq(SfSecurityPolicy::getStatus, STATUS_ACTIVE));

        return policies.stream()
            .flatMap(policy -> validatePolicyRules(policy, resourceAttributes).stream())
            .toList();
    }

    /**
     * Get all active policies of a specific type.
     */
    public List<SfSecurityPolicy> listActivePoliciesByType(String policyType) {
        return baseMapper.selectList(
            new LambdaQueryWrapper<SfSecurityPolicy>()
                .eq(SfSecurityPolicy::getPolicyType, policyType)
                .eq(SfSecurityPolicy::getStatus, STATUS_ACTIVE)
                .orderByDesc(SfSecurityPolicy::getCreatedAt));
    }

    /**
     * Get the latest active policy by name.
     */
    public Optional<SfSecurityPolicy> getActivePolicyByName(String name) {
        List<SfSecurityPolicy> policies = baseMapper.selectList(
            new LambdaQueryWrapper<SfSecurityPolicy>()
                .eq(SfSecurityPolicy::getName, name)
                .eq(SfSecurityPolicy::getStatus, STATUS_ACTIVE)
                .orderByDesc(SfSecurityPolicy::getCreatedAt)
                .last("LIMIT 1"));
        return policies.isEmpty() ? Optional.empty() : Optional.of(policies.get(0));
    }

    /**
     * Check if a resource attribute satisfies all active policies of a given type.
     */
    public boolean isCompliant(String policyType, Map<String, Object> resourceAttributes) {
        return validateAgainstPolicies(policyType, resourceAttributes).isEmpty();
    }

    private List<String> validatePolicyRules(SfSecurityPolicy policy, Map<String, Object> resourceAttributes) {
        List<String> violations = new java.util.ArrayList<>();
        if (policy.getRulesJson() == null || policy.getRulesJson().isBlank()) {
            return violations;
        }
        try {
            List<PolicyRule> rules = objectMapper.readValue(policy.getRulesJson(), new TypeReference<List<PolicyRule>>() {});
            for (PolicyRule rule : rules) {
                Object actualValue = resourceAttributes.get(rule.attribute());
                if (!evaluateRule(rule, actualValue)) {
                    violations.add(String.format("Policy '%s' violated: %s %s %s (actual: %s)",
                        policy.getName(), rule.attribute(), rule.operator(), rule.expectedValue(), actualValue));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse rules JSON for policy {}: {}", policy.getId(), e.getMessage());
            violations.add("Policy '" + policy.getName() + "' has invalid rules configuration");
        }
        return violations;
    }

    private boolean evaluateRule(PolicyRule rule, Object actualValue) {
        if (actualValue == null) {
            return "is_null".equals(rule.operator()) || "null".equals(rule.operator());
        }
        String expected = rule.expectedValue() != null ? rule.expectedValue().toString() : "";
        String actual = actualValue.toString();
        return switch (rule.operator()) {
            case "eq", "equals", "==" -> actual.equals(expected);
            case "ne", "not_equals", "!=" -> !actual.equals(expected);
            case "contains" -> actual.contains(expected);
            case "starts_with" -> actual.startsWith(expected);
            case "ends_with" -> actual.endsWith(expected);
            case "gt", ">" -> compareNumeric(actual, expected) > 0;
            case "gte", ">=" -> compareNumeric(actual, expected) >= 0;
            case "lt", "<" -> compareNumeric(actual, expected) < 0;
            case "lte", "<=" -> compareNumeric(actual, expected) <= 0;
            case "in" -> expected.contains(actual);
            case "not_null", "is_not_null" -> true;
            default -> true;
        };
    }

    private int compareNumeric(String actual, String expected) {
        try {
            double a = Double.parseDouble(actual);
            double e = Double.parseDouble(expected);
            return Double.compare(a, e);
        } catch (NumberFormatException ex) {
            return actual.compareTo(expected);
        }
    }

    private void validatePolicy(SfSecurityPolicy policy) {
        if (policy.getName() == null || policy.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Policy name is required");
        }
        if (policy.getName().length() > 128) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Policy name must not exceed 128 characters");
        }
        if (policy.getPolicyType() == null || policy.getPolicyType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Policy type is required");
        }
    }

    private void validateRulesJson(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return;
        }
        try {
            objectMapper.readValue(rulesJson, new TypeReference<List<PolicyRule>>() {});
        } catch (Exception e) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Invalid rules JSON: " + e.getMessage());
        }
    }

    public record PolicyRule(String attribute, String operator, Object expectedValue) {
    }
}
