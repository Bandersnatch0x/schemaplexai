package com.schemaplexai.spec.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpecRoleAuthorityMapperTest {

    @Test
    void adminRoleCode_grantsFullMatrix() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("ADMIN"));

        assertThat(authorities).contains(
                "admin",
                SpecAuthorities.WRITE,
                SpecAuthorities.REVIEW,
                SpecAuthorities.PUBLISH,
                SpecAuthorities.ROLLBACK,
                SpecAuthorities.DELETE);
    }

    @Test
    void adminVariants_allGrantFullMatrix() {
        for (String code : Arrays.asList("tenant_admin", "SUPER_ADMIN", "spec_admin")) {
            Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of(code));

            assertThat(authorities)
                    .as("role %s", code)
                    .contains(SpecAuthorities.ROLLBACK, SpecAuthorities.DELETE, SpecAuthorities.PUBLISH);
        }
    }

    @Test
    void editorRoleCode_grantsWriteOnly() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("Editor"));

        assertThat(authorities).contains("editor", SpecAuthorities.WRITE);
        assertThat(authorities).doesNotContain(
                SpecAuthorities.PUBLISH,
                SpecAuthorities.ROLLBACK,
                SpecAuthorities.DELETE,
                SpecAuthorities.REVIEW);
    }

    @Test
    void approverRoleCode_grantsReviewAndPublish() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("approver"));

        assertThat(authorities).contains(SpecAuthorities.REVIEW, SpecAuthorities.PUBLISH);
        assertThat(authorities).doesNotContain(SpecAuthorities.WRITE, SpecAuthorities.ROLLBACK, SpecAuthorities.DELETE);
    }

    @Test
    void reviewerRoleCode_grantsReviewOnly() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("spec_reviewer"));

        assertThat(authorities).containsExactlyInAnyOrder("spec_reviewer", SpecAuthorities.REVIEW);
    }

    @Test
    void unknownRoleCode_isExposedVerbatimWithoutPrivileges() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("billing_viewer"));

        assertThat(authorities).containsExactly("billing_viewer");
    }

    @Test
    void permissionStyleCode_passesThroughUnchanged() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("spec:rollback"));

        assertThat(authorities).contains(SpecAuthorities.ROLLBACK);
    }

    @Test
    void nullAndBlankInputs_areIgnored() {
        assertThat(SpecRoleAuthorityMapper.toAuthorities(null)).isEmpty();
        assertThat(SpecRoleAuthorityMapper.toAuthorities(Collections.emptyList())).isEmpty();
        assertThat(SpecRoleAuthorityMapper.toAuthorities(Arrays.asList(null, "  "))).isEmpty();
    }

    @Test
    void multipleRoles_unionTheirGrants() {
        Set<String> authorities = SpecRoleAuthorityMapper.toAuthorities(List.of("editor", "approver"));

        assertThat(authorities).contains(SpecAuthorities.WRITE, SpecAuthorities.REVIEW, SpecAuthorities.PUBLISH);
        assertThat(authorities).doesNotContain(SpecAuthorities.ROLLBACK, SpecAuthorities.DELETE);
    }
}
