package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.system.entity.SfRole;
import com.schemaplexai.system.mapper.SfRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private SfRoleMapper roleMapper;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    @Test
    void getById_returnsRole() {
        SfRole role = new SfRole();
        role.setId(1L);
        role.setName("Admin");
        role.setCode("admin");
        when(roleMapper.selectById(1L)).thenReturn(role);

        SfRole result = roleService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Admin");
        assertThat(result.getCode()).isEqualTo("admin");
    }

    @Test
    void getById_returnsNullWhenNotFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        SfRole result = roleService.getById(1L);

        assertThat(result).isNull();
    }

    @Test
    void save_success_returnsTrue() {
        SfRole role = new SfRole();
        role.setName("User");
        role.setCode("user");
        when(roleMapper.insert(any(SfRole.class))).thenReturn(1);

        boolean result = roleService.save(role);

        assertThat(result).isTrue();
        verify(roleMapper).insert(role);
    }

    @Test
    void updateById_success_returnsTrue() {
        SfRole role = new SfRole();
        role.setId(1L);
        role.setName("Updated Role");
        when(roleMapper.updateById(any(SfRole.class))).thenReturn(1);

        boolean result = roleService.updateById(role);

        assertThat(result).isTrue();
        verify(roleMapper).updateById(role);
    }

    @Test
    void page_returnsPageResult() {
        SfRole role = new SfRole();
        role.setId(1L);
        role.setName("Admin");
        Page<SfRole> page = new Page<>();
        page.setRecords(List.of(role));
        page.setTotal(1);
        when(roleMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Page<SfRole> result = roleService.page(new Page<>(1, 10));

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void list_returnsAllRoles() {
        SfRole r1 = new SfRole();
        r1.setId(1L);
        SfRole r2 = new SfRole();
        r2.setId(2L);
        when(roleMapper.selectList(any())).thenReturn(List.of(r1, r2));

        List<SfRole> result = roleService.list();

        assertThat(result).hasSize(2);
    }
}
