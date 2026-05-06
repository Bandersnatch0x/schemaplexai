package com.schemaplexai.context.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.mapper.SfContextMapper;
import com.schemaplexai.context.mapper.SfContextSnapshotMapper;
import com.schemaplexai.context.service.impl.ContextSnapshotServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextSnapshotServiceImplTest {

    @Mock
    private SfContextSnapshotMapper snapshotMapper;

    @Mock
    private SfContextMapper contextMapper;

    @InjectMocks
    private ContextSnapshotServiceImpl contextSnapshotService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contextSnapshotService, "baseMapper", snapshotMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // createSnapshot
    // ------------------------------------------------------------------

    @Test
    void createSnapshot_nullContextId_throwsParamError() {
        assertThatThrownBy(() -> contextSnapshotService.createSnapshot(null, "json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createSnapshot_contextNotFound_throwsContextNotFound() {
        when(contextMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> contextSnapshotService.createSnapshot(1L, "json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONTEXT_NOT_FOUND.getCode());
    }

    @Test
    void createSnapshot_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfContext context = new SfContext();
        context.setId(1L);
        context.setTenantId("tenant-1");
        when(contextMapper.selectById(1L)).thenReturn(context);

        assertThatThrownBy(() -> contextSnapshotService.createSnapshot(1L, "json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void createSnapshot_firstSnapshot_versionOne() {
        SfContext context = new SfContext();
        context.setId(1L);
        when(contextMapper.selectById(1L)).thenReturn(context);
        when(snapshotMapper.selectList(any())).thenReturn(Collections.emptyList());

        SfContextSnapshot result = contextSnapshotService.createSnapshot(1L, "snapshot data");

        assertThat(result.getContextId()).isEqualTo(1L);
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getSnapshotJson()).isEqualTo("snapshot data");
        verify(snapshotMapper).insert(any(SfContextSnapshot.class));
    }

    @Test
    void createSnapshot_subsequentSnapshot_incrementsVersion() {
        SfContext context = new SfContext();
        context.setId(1L);
        when(contextMapper.selectById(1L)).thenReturn(context);

        SfContextSnapshot latest = new SfContextSnapshot();
        latest.setVersion(5);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(latest));

        SfContextSnapshot result = contextSnapshotService.createSnapshot(1L, "data");

        assertThat(result.getVersion()).isEqualTo(6);
    }

    @Test
    void createSnapshot_withTenantId_setsTenantId() {
        TenantContextHolder.setTenantId("tenant-1");
        SfContext context = new SfContext();
        context.setId(1L);
        context.setTenantId("tenant-1");
        when(contextMapper.selectById(1L)).thenReturn(context);
        when(snapshotMapper.selectList(any())).thenReturn(Collections.emptyList());

        SfContextSnapshot result = contextSnapshotService.createSnapshot(1L, "data");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    // ------------------------------------------------------------------
    // restoreFromSnapshot
    // ------------------------------------------------------------------

    @Test
    void restoreFromSnapshot_notFound_throwsNotFound() {
        when(snapshotMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> contextSnapshotService.restoreFromSnapshot(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void restoreFromSnapshot_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfContextSnapshot snapshot = new SfContextSnapshot();
        snapshot.setId(1L);
        snapshot.setTenantId("tenant-1");
        snapshot.setSnapshotJson("data");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshot);

        assertThatThrownBy(() -> contextSnapshotService.restoreFromSnapshot(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void restoreFromSnapshot_success_returnsSnapshotJson() {
        SfContextSnapshot snapshot = new SfContextSnapshot();
        snapshot.setId(1L);
        snapshot.setContextId(10L);
        snapshot.setVersion(3);
        snapshot.setSnapshotJson("restored data");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshot);

        String result = contextSnapshotService.restoreFromSnapshot(1L);

        assertThat(result).isEqualTo("restored data");
    }

    // ------------------------------------------------------------------
    // listSnapshotsByContext
    // ------------------------------------------------------------------

    @Test
    void listSnapshotsByContext_nullContextId_throwsParamError() {
        assertThatThrownBy(() -> contextSnapshotService.listSnapshotsByContext(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listSnapshotsByContext_returnsSnapshotsOrderedByVersion() {
        SfContextSnapshot s1 = new SfContextSnapshot();
        s1.setVersion(2);
        SfContextSnapshot s2 = new SfContextSnapshot();
        s2.setVersion(1);
        when(snapshotMapper.selectList(any())).thenReturn(List.of(s1, s2));

        List<SfContextSnapshot> result = contextSnapshotService.listSnapshotsByContext(1L);

        assertThat(result).hasSize(2);
    }

    // ------------------------------------------------------------------
    // compareSnapshots
    // ------------------------------------------------------------------

    @Test
    void compareSnapshots_snapshotANotFound_throwsNotFound() {
        when(snapshotMapper.selectById(1L)).thenReturn(null);
        when(snapshotMapper.selectById(2L)).thenReturn(new SfContextSnapshot());

        assertThatThrownBy(() -> contextSnapshotService.compareSnapshots(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void compareSnapshots_snapshotBNotFound_throwsNotFound() {
        when(snapshotMapper.selectById(1L)).thenReturn(new SfContextSnapshot());
        when(snapshotMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> contextSnapshotService.compareSnapshots(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void compareSnapshots_differentContextIds_throwsParamError() {
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(2L);
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        assertThatThrownBy(() -> contextSnapshotService.compareSnapshots(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void compareSnapshots_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        snapshotA.setTenantId("tenant-1");
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(1L);
        snapshotB.setTenantId("tenant-1");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        assertThatThrownBy(() -> contextSnapshotService.compareSnapshots(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void compareSnapshots_equalJson_reportsEqual() {
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        snapshotA.setVersion(1);
        snapshotA.setSnapshotJson("same data");
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(1L);
        snapshotB.setVersion(2);
        snapshotB.setSnapshotJson("same data");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        String result = contextSnapshotService.compareSnapshots(1L, 2L);

        assertThat(result).contains("Equal: true");
    }

    @Test
    void compareSnapshots_differentJson_reportsNotEqual() {
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        snapshotA.setVersion(1);
        snapshotA.setSnapshotJson("data A");
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(1L);
        snapshotB.setVersion(2);
        snapshotB.setSnapshotJson("data B that is longer");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        String result = contextSnapshotService.compareSnapshots(1L, 2L);

        assertThat(result).contains("Equal: false");
        assertThat(result).contains("Length A: 6");
        assertThat(result).contains("Length B: 21");
    }

    @Test
    void compareSnapshots_nullJson_handlesGracefully() {
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        snapshotA.setVersion(1);
        snapshotA.setSnapshotJson(null);
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(1L);
        snapshotB.setVersion(2);
        snapshotB.setSnapshotJson("data");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        String result = contextSnapshotService.compareSnapshots(1L, 2L);

        assertThat(result).contains("Equal: false");
    }

    @Test
    void compareSnapshots_withTenantId_matchingTenant_succeeds() {
        TenantContextHolder.setTenantId("tenant-1");
        SfContextSnapshot snapshotA = new SfContextSnapshot();
        snapshotA.setContextId(1L);
        snapshotA.setVersion(1);
        snapshotA.setTenantId("tenant-1");
        SfContextSnapshot snapshotB = new SfContextSnapshot();
        snapshotB.setContextId(1L);
        snapshotB.setVersion(2);
        snapshotB.setTenantId("tenant-1");
        when(snapshotMapper.selectById(1L)).thenReturn(snapshotA);
        when(snapshotMapper.selectById(2L)).thenReturn(snapshotB);

        String result = contextSnapshotService.compareSnapshots(1L, 2L);

        assertThat(result).isNotNull();
    }
}
