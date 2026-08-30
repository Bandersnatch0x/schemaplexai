package com.schemaplexai.system.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.mapper.SfTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private SfTenantMapper tenantMapper;

    @Mock
    private TenantCacheSyncer tenantCacheSyncer;

    private TenantService tenantService;

    private SfTenant sampleTenant;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantCacheSyncer);
        // ServiceImpl stores the mapper in the baseMapper field
        ReflectionTestUtils.setField(tenantService, "baseMapper", tenantMapper);

        sampleTenant = new SfTenant();
        sampleTenant.setId(1L);
        sampleTenant.setName("Test Tenant");
        sampleTenant.setCode("TEST");
        sampleTenant.setStatus("ACTIVE");
        sampleTenant.setConfigJson("{}");
    }

    @Test
    void getValidTenant_existingActiveTenant_returnsTenant() {
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        SfTenant result = tenantService.getValidTenant(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Tenant");
        assertThat(result.getCode()).isEqualTo("TEST");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getValidTenant_nonExistentTenant_throwsTenantNotFound() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> tenantService.getValidTenant(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_NOT_FOUND.getCode());
    }

    @Test
    void getValidTenant_disabledTenant_throwsTenantDisabled() {
        sampleTenant.setStatus("DISABLED");
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        assertThatThrownBy(() -> tenantService.getValidTenant(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TENANT_DISABLED.getCode());
    }

    @Test
    void getValidTenant_nullStatus_doesNotThrow() {
        sampleTenant.setStatus(null);
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        SfTenant result = tenantService.getValidTenant(1L);

        assertThat(result).isNotNull();
    }

    // ------------------------------------------------------------------
    // Write-through to the gateway tenant cache channel (issue 913)
    // ------------------------------------------------------------------

    @Test
    void createTenant_persistsAndSyncsCache() {
        when(tenantMapper.insert(any())).thenReturn(1);

        boolean created = tenantService.createTenant(sampleTenant);

        assertThat(created).isTrue();
        verify(tenantCacheSyncer).sync(sampleTenant);
    }

    @Test
    void createTenant_failure_doesNotSync() {
        when(tenantMapper.insert(any())).thenReturn(0);

        boolean created = tenantService.createTenant(sampleTenant);

        assertThat(created).isFalse();
        verify(tenantCacheSyncer, never()).sync(any());
    }

    @Test
    void updateTenant_republishesCurrentStatus() {
        when(tenantMapper.updateById(any())).thenReturn(1);
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);

        SfTenant partial = new SfTenant();
        partial.setId(1L);
        partial.setConfigJson("{\"updated\":true}");

        boolean updated = tenantService.updateTenant(partial);

        assertThat(updated).isTrue();
        // The entity is re-read so partial updates publish the true current status.
        verify(tenantCacheSyncer).sync(sampleTenant);
    }

    @Test
    void deleteTenant_evictsCacheEntry() {
        when(tenantMapper.selectById(1L)).thenReturn(sampleTenant);
        // MyBatis-Plus removeById(Serializable) routes to the entity-based overload
        when(tenantMapper.deleteById(any(SfTenant.class))).thenReturn(1);

        boolean deleted = tenantService.deleteTenant(1L);

        assertThat(deleted).isTrue();
        verify(tenantCacheSyncer).evict(org.mockito.ArgumentMatchers.any(SfTenant.class));
    }
}
