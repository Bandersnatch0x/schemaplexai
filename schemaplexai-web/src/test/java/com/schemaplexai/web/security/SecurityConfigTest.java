package com.schemaplexai.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void securityFilterChainBeanExists() throws Exception {
        SecurityConfig config = new SecurityConfig();
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain expectedChain = new DefaultSecurityFilterChain(
                request -> true,
                java.util.Collections.emptyList()
        );
        when(httpSecurity.build()).thenReturn(expectedChain);

        SecurityFilterChain chain = config.filterChain(httpSecurity);
        assertThat(chain).isNotNull();
    }
}
