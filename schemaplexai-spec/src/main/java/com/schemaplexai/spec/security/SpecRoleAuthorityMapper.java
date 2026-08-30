package com.schemaplexai.spec.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps tenant role codes (sf_role.code) to Spec-domain authorities.
 * <p>
 * Two granting paths coexist:
 * <ol>
 *   <li><b>Role bridging</b> — well-known role codes from spec-management §3.2
 *       (editor / approver / reviewer / admin, optionally prefixed
 *       {@code spec_} or {@code tenant_}/{@code super_} for admins) expand to
 *       the corresponding {@code spec:*} authorities.</li>
 *   <li><b>Permission passthrough</b> — any code that already looks like an
 *       authority (contains {@code ':'}, e.g. {@code spec:publish} granted via
 *       sf_permission → sf_role_permission) is passed through unchanged, so
 *       operators can tailor the matrix purely through RBAC data.</li>
 * </ol>
 * Every role code is also exposed verbatim (lower-cased), so
 * {@code hasAuthority('admin')}-style checks remain possible.
 */
public final class SpecRoleAuthorityMapper {

    private static final Set<String> ALL_OPERATIONS = Set.of(
            SpecAuthorities.WRITE,
            SpecAuthorities.REVIEW,
            SpecAuthorities.PUBLISH,
            SpecAuthorities.ROLLBACK,
            SpecAuthorities.DELETE);

    private static final Map<String, Set<String>> ROLE_GRANTS = Map.ofEntries(
            // admins: full matrix including rollback and delete
            Map.entry("admin", ALL_OPERATIONS),
            Map.entry("spec_admin", ALL_OPERATIONS),
            Map.entry("tenant_admin", ALL_OPERATIONS),
            Map.entry("super_admin", ALL_OPERATIONS),
            // editors: create / edit / new versions
            Map.entry("editor", Set.of(SpecAuthorities.WRITE)),
            Map.entry("spec_editor", Set.of(SpecAuthorities.WRITE)),
            // approvers: review + publish
            Map.entry("approver", Set.of(SpecAuthorities.REVIEW, SpecAuthorities.PUBLISH)),
            Map.entry("spec_approver", Set.of(SpecAuthorities.REVIEW, SpecAuthorities.PUBLISH)),
            // reviewers: review decisions only
            Map.entry("reviewer", Set.of(SpecAuthorities.REVIEW)),
            Map.entry("spec_reviewer", Set.of(SpecAuthorities.REVIEW)));

    private SpecRoleAuthorityMapper() {
    }

    /**
     * Expand role codes into the granted authority set. Null/blank inputs are
     * ignored; matching is case-insensitive.
     */
    public static Set<String> toAuthorities(Collection<String> roleCodes) {
        Set<String> authorities = new LinkedHashSet<>();
        if (roleCodes == null) {
            return authorities;
        }
        for (String raw : roleCodes) {
            if (raw == null) {
                continue;
            }
            String code = raw.trim().toLowerCase(Locale.ROOT);
            if (code.isEmpty()) {
                continue;
            }
            // Role code itself (and permission-style codes) stay usable verbatim.
            authorities.add(code);
            Set<String> grants = ROLE_GRANTS.get(code);
            if (grants != null) {
                authorities.addAll(grants);
            }
        }
        return authorities;
    }
}
