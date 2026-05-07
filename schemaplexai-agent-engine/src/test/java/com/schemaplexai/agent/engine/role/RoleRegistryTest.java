package com.schemaplexai.agent.engine.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentRole;
import com.schemaplexai.agent.engine.mapper.SfAgentRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleRegistryTest {

    @Mock
    private SfAgentRoleMapper roleMapper;

    private RoleRegistry registry;
    private static final String TENANT = "tenant-1";
    private static final String ROLE_NAME = "senior-dev";

    @BeforeEach
    void setUp() {
        registry = new RoleRegistry(roleMapper);
    }

    @Test
    void shouldReturnNullWhenRoleNotFound() {
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RoleOverlay result = registry.resolve(ROLE_NAME, TENANT);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnRoleOverlayWhenFound() {
        SfAgentRole entity = new SfAgentRole();
        entity.setName(ROLE_NAME);
        entity.setDescription("Senior developer role");
        entity.setOverlay("You are a senior developer with 10 years experience...");
        entity.setStatus(1);

        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        RoleOverlay result = registry.resolve(ROLE_NAME, TENANT);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(ROLE_NAME);
        assertThat(result.description()).isEqualTo("Senior developer role");
        assertThat(result.overlay()).isEqualTo("You are a senior developer with 10 years experience...");
    }
}
