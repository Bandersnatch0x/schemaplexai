package com.schemaplexai.spec.security;

import com.schemaplexai.spec.mapper.SpecAuthMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecRoleProviderTest {

    @Mock
    private SpecAuthMapper authMapper;

    @InjectMocks
    private SpecRoleProvider roleProvider;

    @Test
    void authoritiesFor_mergesBridgedRoleGrantsAndPermissionCodes() {
        when(authMapper.selectRoleCodes(42L)).thenReturn(List.of("editor"));
        when(authMapper.selectPermissionCodes(42L)).thenReturn(List.of("spec:publish"));

        Set<String> authorities = roleProvider.authoritiesFor("42", "7");

        assertThat(authorities).contains("editor", SpecAuthorities.WRITE, "spec:publish");
        assertThat(authorities).doesNotContain(SpecAuthorities.ROLLBACK, SpecAuthorities.DELETE);
    }

    @Test
    void authoritiesFor_adminRole_grantsFullMatrix() {
        when(authMapper.selectRoleCodes(1L)).thenReturn(List.of("admin"));
        when(authMapper.selectPermissionCodes(1L)).thenReturn(List.of());

        Set<String> authorities = roleProvider.authoritiesFor("1", "7");

        assertThat(authorities).contains(
                SpecAuthorities.WRITE,
                SpecAuthorities.REVIEW,
                SpecAuthorities.PUBLISH,
                SpecAuthorities.ROLLBACK,
                SpecAuthorities.DELETE);
    }

    @Test
    void authoritiesFor_userWithoutRoles_hasNoPrivileges() {
        when(authMapper.selectRoleCodes(9L)).thenReturn(List.of());
        when(authMapper.selectPermissionCodes(9L)).thenReturn(List.of());

        assertThat(roleProvider.authoritiesFor("9", "7")).isEmpty();
    }

    @Test
    void authoritiesFor_nonNumericUserId_skipsDb() {
        assertThat(roleProvider.authoritiesFor("service-account", "7")).isEmpty();
        verifyNoInteractions(authMapper);
    }

    @Test
    void authoritiesFor_blankUserId_skipsDb() {
        assertThat(roleProvider.authoritiesFor(" ", "7")).isEmpty();
        verifyNoInteractions(authMapper);
    }

    @Test
    void authoritiesFor_secondCallWithinTtl_usesCache() {
        when(authMapper.selectRoleCodes(42L)).thenReturn(List.of("editor"));
        when(authMapper.selectPermissionCodes(42L)).thenReturn(List.of());

        roleProvider.authoritiesFor("42", "7");
        roleProvider.authoritiesFor("42", "7");

        verify(authMapper, times(1)).selectRoleCodes(42L);
        verify(authMapper, times(1)).selectPermissionCodes(42L);
    }

    @Test
    void authoritiesFor_dbFailureWithoutPriorCache_degradesToEmpty() {
        when(authMapper.selectRoleCodes(anyLong())).thenThrow(new RuntimeException("db down"));

        assertThat(roleProvider.authoritiesFor("42", "7")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void authoritiesFor_dbFailureWithPriorCache_returnsLastKnown() throws Exception {
        when(authMapper.selectRoleCodes(42L))
                .thenReturn(List.of("editor"))
                .thenThrow(new RuntimeException("db down"));
        when(authMapper.selectPermissionCodes(42L)).thenReturn(List.of());

        Set<String> first = roleProvider.authoritiesFor("42", "7");
        assertThat(first).contains(SpecAuthorities.WRITE);

        // Expire the cache entry so the next call must hit the (failing) DB.
        Map<String, ?> cache = (Map<String, ?>) org.springframework.test.util.ReflectionTestUtils
                .getField(roleProvider, "cache");
        assertThat(cache).isInstanceOf(ConcurrentHashMap.class);
        Map.Entry<String, ?> entry = cache.entrySet().iterator().next();
        Class<?> cacheEntryClass = entry.getValue().getClass();
        Constructor<?> ctor = cacheEntryClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object expired = ctor.newInstance(0L, Set.of("editor"), Set.of());
        ((Map<String, Object>) cache).put(entry.getKey(), expired);

        Set<String> second = roleProvider.authoritiesFor("42", "7");

        assertThat(second).contains("editor", SpecAuthorities.WRITE);
    }
}
