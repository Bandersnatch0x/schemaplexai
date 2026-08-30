package com.schemaplexai.gateway.tenant;

import reactor.core.publisher.Mono;

/**
 * Validates a tenant identifier at the gateway edge (issue 913, spec §4.3).
 * Implementations must never block; errors are propagated as Mono errors and
 * are handled fail-closed by the caller.
 */
public interface ReactiveTenantValidator {

    /**
     * Resolve the status of the given tenant.
     *
     * @param tenantId the tenant identifier carried by the request (tenant code)
     * @return ACTIVE, DISABLED or NOT_FOUND
     */
    Mono<TenantStatus> validate(String tenantId);
}
