package com.schemaplexai.spec.security;

import com.schemaplexai.common.context.TenantContextHolder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-32b";

    @Mock
    private SpecRoleProvider roleProvider;

    private GatewayAuthFilter filter;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter(SECRET, roleProvider);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private String token(String userId, String tenantId) {
        return Jwts.builder()
                .subject(userId)
                .claim("tenantId", tenantId)
                .claim("username", "alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void validToken_buildsAuthenticatedPrincipalWithResolvedAuthorities() {
        when(roleProvider.authoritiesFor("42", "7")).thenReturn(Set.of("editor", "spec:write"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token("42", "7"));

        Optional<Authentication> authentication = filter.buildAuthentication(request);

        assertThat(authentication).isPresent();
        assertThat(authentication.get().getPrincipal()).isEqualTo("42");
        assertThat(authentication.get().isAuthenticated()).isTrue();
        Set<String> authorities = authentication.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder("editor", "spec:write");
        Map<?, ?> details = (Map<?, ?>) authentication.get().getDetails();
        assertThat(details.get("tenantId")).isEqualTo("7");
        assertThat(details.get("username")).isEqualTo("alice");
    }

    @Test
    void rolesClaimInToken_bypassesDbLookup() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String jwt = Jwts.builder()
                .subject("42")
                .claim("tenantId", "7")
                .claim("roles", List.of("approver"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
        request.addHeader("Authorization", "Bearer " + jwt);

        Optional<Authentication> authentication = filter.buildAuthentication(request);

        assertThat(authentication).isPresent();
        Set<String> authorities = authentication.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).contains("approver", SpecAuthorities.PUBLISH, SpecAuthorities.REVIEW);
        verify(roleProvider, never()).authoritiesFor(anyString(), anyString());
    }

    @Test
    void missingAuthorizationHeader_staysAnonymous() {
        assertThat(filter.buildAuthentication(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void nonBearerScheme_staysAnonymous() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertThat(filter.buildAuthentication(request)).isEmpty();
    }

    @Test
    void tamperedToken_isRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String jwt = token("42", "7");
        request.addHeader("Authorization", "Bearer " + jwt.substring(0, jwt.length() - 2) + "xx");

        assertThat(filter.buildAuthentication(request)).isEmpty();
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-that-is-long-enough-32b!".getBytes(StandardCharsets.UTF_8));
        String jwt = Jwts.builder()
                .subject("42")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);

        assertThat(filter.buildAuthentication(request)).isEmpty();
    }

    @Test
    void expiredToken_isRejected() {
        String jwt = Jwts.builder()
                .subject("42")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);

        assertThat(filter.buildAuthentication(request)).isEmpty();
    }

    @Test
    void tokenWithoutSubject_isRejected() {
        String jwt = Jwts.builder()
                .claim("tenantId", "7")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);

        assertThat(filter.buildAuthentication(request)).isEmpty();
    }
}
