package com.schemaplexai.system.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Accepts the gateway-validated bearer token on downstream calls. The gateway
 * verifies signatures and injects identity headers, but this module also runs
 * Spring Security with {@code anyRequest().authenticated()} — without a filter
 * that populates the SecurityContext every forwarded request is anonymous and
 * gets 403 (live defect from browser verification round 2).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                String userId = claims.getSubject();
                String username = claims.get("username", String.class);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(username);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("Bearer token not accepted by system module: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
