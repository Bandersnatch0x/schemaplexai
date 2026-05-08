package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.system.entity.SfPermission;
import com.schemaplexai.system.mapper.SfPermissionMapper;
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
class PermissionServiceTest {

    @Mock
    private SfPermissionMapper permissionMapper;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService();
        ReflectionTestUtils.setField(permissionService, "baseMapper", permissionMapper);
    }

    @Test
    void getById_returnsPermission() {
        SfPermission permission = new SfPermission();
        permission.setId(1L);
        permission.setName("Read Users");
        permission.setCode("user:read");
        when(permissionMapper.selectById(1L)).thenReturn(permission);

        SfPermission result = permissionService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Read Users");
        assertThat(result.getCode()).isEqualTo("user:read");
    }

    @Test
    void getById_returnsNullWhenNotFound() {
        when(permissionMapper.selectById(1L)).thenReturn(null);

        SfPermission result = permissionService.getById(1L);

        assertThat(result).isNull();
    }

    @Test
    void save_success_returnsTrue() {
        SfPermission permission = new SfPermission();
        permission.setName("Write Users");
        permission.setCode("user:write");
        when(permissionMapper.insert(any(SfPermission.class))).thenReturn(1);

        boolean result = permissionService.save(permission);

        assertThat(result).isTrue();
        verify(permissionMapper).insert(permission);
    }

    @Test
    void updateById_success_returnsTrue() {
        SfPermission permission = new SfPermission();
        permission.setId(1L);
        permission.setName("Updated Permission");
        when(permissionMapper.updateById(any(SfPermission.class))).thenReturn(1);

        boolean result = permissionService.updateById(permission);

        assertThat(result).isTrue();
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void page_returnsPageResult() {
        SfPermission permission = new SfPermission();
        permission.setId(1L);
        permission.setName("Read Users");
        Page<SfPermission> page = new Page<>();
        page.setRecords(List.of(permission));
        page.setTotal(1);
        when(permissionMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Page<SfPermission> result = permissionService.page(new Page<>(1, 10));

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void list_returnsAllPermissions() {
        SfPermission p1 = new SfPermission();
        p1.setId(1L);
        SfPermission p2 = new SfPermission();
        p2.setId(2L);
        when(permissionMapper.selectList(any())).thenReturn(List.of(p1, p2));

        List<SfPermission> result = permissionService.list();

        assertThat(result).hasSize(2);
    }
}
