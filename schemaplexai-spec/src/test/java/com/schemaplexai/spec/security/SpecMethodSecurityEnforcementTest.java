package com.schemaplexai.spec.security;

import com.schemaplexai.spec.controller.SpecController;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.service.SpecService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Boots a real method-security context around {@link SpecController} and proves
 * the {@code @PreAuthorize} matrix is enforced, not decorative: a principal
 * without the required authority is denied, one with it proceeds.
 */
class SpecMethodSecurityEnforcementTest {

    private AnnotationConfigApplicationContext context;
    private SpecService specService;
    private SpecController controller;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestSecurityConfig.class);
        context.registerBean("specController", SpecController.class,
                () -> new SpecController(context.getBean(SpecService.class)));
        context.refresh();
        specService = context.getBean(SpecService.class);
        controller = context.getBean(SpecController.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    private void authenticate(String... authorities) {
        List<SimpleGrantedAuthority> granted = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "n/a", granted);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void publish_deniedWithoutPublishAuthority() {
        authenticate(SpecAuthorities.WRITE); // editor, not approver

        assertThatThrownBy(() -> controller.publishSpec(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void publish_allowedWithPublishAuthority() {
        when(specService.publishSpec(anyLong())).thenReturn(new SfSpecVersion());
        authenticate(SpecAuthorities.PUBLISH);

        assertThatCode(() -> controller.publishSpec(1L)).doesNotThrowAnyException();
        verify(specService).publishSpec(1L);
    }

    @Test
    void delete_deniedWithoutDeleteAuthority() {
        authenticate(SpecAuthorities.WRITE, SpecAuthorities.PUBLISH);

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void delete_allowedWithDeleteAuthority() {
        when(specService.deleteSpec(anyLong())).thenReturn(true);
        authenticate(SpecAuthorities.DELETE);

        assertThatCode(() -> controller.delete(1L)).doesNotThrowAnyException();
        verify(specService).deleteSpec(1L);
    }

    @Test
    void update_deniedWithoutWriteAuthority() {
        authenticate(SpecAuthorities.PUBLISH);

        assertThatThrownBy(() -> controller.update(1L, new SfSpec()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rollback_deniedWithoutRollbackAuthority() {
        authenticate(SpecAuthorities.WRITE, SpecAuthorities.PUBLISH);

        assertThatThrownBy(() -> controller.rollbackSpec(1L, 5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rollback_allowedWithRollbackAuthority() {
        when(specService.rollbackSpec(anyLong(), anyLong())).thenReturn(new SfSpec());
        authenticate(SpecAuthorities.ROLLBACK);

        assertThatCode(() -> controller.rollbackSpec(1L, 5L)).doesNotThrowAnyException();
        verify(specService).rollbackSpec(1L, 5L);
    }

    @Test
    void unauthenticated_isDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.publishSpec(1L))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SpecService specService() {
            return mock(SpecService.class);
        }
    }
}
