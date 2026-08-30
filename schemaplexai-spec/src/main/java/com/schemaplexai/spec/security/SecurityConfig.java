package com.schemaplexai.spec.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration of the spec service (issue 925 / REQ-07).
 * <p>
 * Authentication is established by {@link GatewayAuthFilter} from the
 * gateway-forwarded signed JWT; this chain makes every business endpoint
 * require an authenticated principal and enables method security so the
 * controllers' {@code @PreAuthorize} matrix (publish / delete / rollback /
 * write / review) is enforced. Documentation endpoints stay public, mirroring
 * the other services.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GatewayAuthFilter gatewayAuthFilter(@Value("${jwt.secret}") String jwtSecret,
                                               SpecRoleProvider roleProvider) {
        return new GatewayAuthFilter(jwtSecret, roleProvider);
    }

    @Bean
    public SecurityFilterChain specSecurityFilterChain(HttpSecurity http,
                                                       GatewayAuthFilter gatewayAuthFilter,
                                                       ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/doc.html",
                                "/webjars/**",
                                "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(objectMapper, response, HttpStatus.UNAUTHORIZED,
                                        ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(objectMapper, response, HttpStatus.FORBIDDEN,
                                        ResultCode.FORBIDDEN)))
                .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(ObjectMapper objectMapper,
                                   HttpServletResponse response,
                                   HttpStatus status,
                                   ResultCode code) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code)));
    }
}
