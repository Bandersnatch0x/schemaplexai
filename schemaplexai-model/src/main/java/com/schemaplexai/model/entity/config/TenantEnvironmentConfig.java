package com.schemaplexai.model.entity.config;

import com.schemaplexai.model.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tenant environment security configuration entity.
 * This is a GLOBAL table — NOT filtered by TenantLineInterceptor.
 * The tenantId field serves as a data identifier, not a filtering condition.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_tenant_environment_config")
public class TenantEnvironmentConfig extends BaseEntity {

    /** Tenant ID (data identifier only — this is a global table) */
    private String tenantId;

    /** Environment identifier: dev / staging / prod */
    private String environment;

    /** Allowed tool names (JSON array string) */
    private String allowedTools;

    /** Security level: LOW / MEDIUM / HIGH / CRITICAL */
    private String securityLevel;

    /** Whether HTTP call tools are allowed */
    private Boolean allowHttpCalls;

    /** Whether file read tools are allowed */
    private Boolean allowFileRead;

    /** Whether irreversible operations are allowed */
    private Boolean allowIrreversibleOps;

    /** Maximum concurrent tool calls permitted */
    private Integer maxConcurrentToolCalls;

    /** Additional configuration (JSON) */
    private String extraConfig;
}
