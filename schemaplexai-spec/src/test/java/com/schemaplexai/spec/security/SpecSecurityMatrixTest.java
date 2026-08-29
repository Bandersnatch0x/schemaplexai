package com.schemaplexai.spec.security;

import com.schemaplexai.spec.controller.SpecController;
import com.schemaplexai.spec.controller.SpecVersionController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the permission matrix wiring (spec-management §3.2, issue 925):
 * publish needs the approver authority, rollback/delete the admin ones,
 * editing the editor one — and method security must actually be enabled so
 * the annotations are enforced.
 */
class SpecSecurityMatrixTest {

    private static String preAuthorizeOn(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertThat(annotation)
                        .as("%s.%s must carry @PreAuthorize", controller.getSimpleName(), methodName)
                        .isNotNull();
                return annotation.value();
            }
        }
        throw new AssertionError("Method " + methodName + " not found on " + controller.getSimpleName());
    }

    @Test
    void methodSecurityIsEnabled() {
        assertThat(SecurityConfig.class).hasAnnotation(EnableMethodSecurity.class);
    }

    @Test
    void publish_requiresApproverAuthority() {
        assertThat(preAuthorizeOn(SpecController.class, "publishSpec"))
                .isEqualTo("hasAuthority('spec:publish')");
    }

    @Test
    void delete_requiresAdminAuthority() {
        assertThat(preAuthorizeOn(SpecController.class, "delete"))
                .isEqualTo("hasAuthority('spec:delete')");
        assertThat(preAuthorizeOn(SpecVersionController.class, "delete"))
                .isEqualTo("hasAuthority('spec:delete')");
    }

    @Test
    void archive_requiresAdminAuthority() {
        assertThat(preAuthorizeOn(SpecController.class, "archiveSpec"))
                .isEqualTo("hasAuthority('spec:delete')");
    }

    @Test
    void editing_requiresEditorAuthority() {
        assertThat(preAuthorizeOn(SpecController.class, "create"))
                .isEqualTo("hasAuthority('spec:write')");
        assertThat(preAuthorizeOn(SpecController.class, "update"))
                .isEqualTo("hasAuthority('spec:write')");
        assertThat(preAuthorizeOn(SpecController.class, "createFromTemplate"))
                .isEqualTo("hasAuthority('spec:write')");
        assertThat(preAuthorizeOn(SpecVersionController.class, "create"))
                .isEqualTo("hasAuthority('spec:write')");
        assertThat(preAuthorizeOn(SpecVersionController.class, "publish"))
                .isEqualTo("hasAuthority('spec:write')");
    }

    @Test
    void diff_isOpenToAnyAuthenticatedReader() {
        for (Method method : SpecVersionController.class.getDeclaredMethods()) {
            if (method.getName().equals("diff")) {
                assertThat(method.getAnnotation(PreAuthorize.class))
                        .as("diff is a reader operation: authentication is enough")
                        .isNull();
                return;
            }
        }
        throw new AssertionError("diff method not found");
    }
}
