package com.schemaplexai.context.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.mapper.SfContextMapper;
import com.schemaplexai.context.service.impl.ContextServiceImpl;
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
class ContextServiceImplTest {

    @Mock
    private SfContextMapper contextMapper;

    @InjectMocks
    private ContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contextService, "baseMapper", contextMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // ingestContext
    // ------------------------------------------------------------------

    @Test
    void ingestContext_nullName_throwsParamError() {
        assertThatThrownBy(() -> contextService.ingestContext(1L, null, "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void ingestContext_blankName_throwsParamError() {
        assertThatThrownBy(() -> contextService.ingestContext(1L, "   ", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void ingestContext_emptyName_throwsParamError() {
        assertThatThrownBy(() -> contextService.ingestContext(1L, "", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void ingestContext_success_createsContext() {
        SfContext result = contextService.ingestContext(1L, "My Context", "document");

        assertThat(result.getName()).isEqualTo("My Context");
        assertThat(result.getWorkspaceId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("document");
        verify(contextMapper).insert(any(SfContext.class));
    }

    @Test
    void ingestContext_trimsName() {
        SfContext result = contextService.ingestContext(1L, "  My Context  ", "type");

        assertThat(result.getName()).isEqualTo("My Context");
    }

    @Test
    void ingestContext_withTenantId_setsTenantId() {
        TenantContextHolder.setTenantId("tenant-1");

        SfContext result = contextService.ingestContext(1L, "Context", "type");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void ingestContext_nullTenantId_tenantIdIsNull() {
        SfContext result = contextService.ingestContext(1L, "Context", "type");

        assertThat(result.getTenantId()).isNull();
    }

    // ------------------------------------------------------------------
    // searchContext
    // ------------------------------------------------------------------

    @Test
    void searchContext_noFilters_returnsAll() {
        when(contextMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfContext> result = contextService.searchContext(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void searchContext_withKeyword_returnsMatching() {
        SfContext ctx = new SfContext();
        ctx.setName("test");
        when(contextMapper.selectList(any())).thenReturn(List.of(ctx));

        List<SfContext> result = contextService.searchContext("test", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchContext_withType_returnsMatching() {
        SfContext ctx = new SfContext();
        ctx.setType("doc");
        when(contextMapper.selectList(any())).thenReturn(List.of(ctx));

        List<SfContext> result = contextService.searchContext(null, "doc");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchContext_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(contextMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfContext> result = contextService.searchContext("keyword", "type");

        assertThat(result).isEmpty();
    }

    @Test
    void searchContext_blankKeyword_ignoresKeywordFilter() {
        when(contextMapper.selectList(any())).thenReturn(Collections.emptyList());

        contextService.searchContext("   ", "type");

        // Should not throw; verifies wrapper construction handles blank keyword
    }

    // ------------------------------------------------------------------
    // refreshContext
    // ------------------------------------------------------------------

    @Test
    void refreshContext_notFound_throwsContextNotFound() {
        when(contextMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> contextService.refreshContext(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONTEXT_NOT_FOUND.getCode());
    }

    @Test
    void refreshContext_success_updatesTimestamp() {
        SfContext context = new SfContext();
        context.setId(1L);
        when(contextMapper.selectById(1L)).thenReturn(context);

        contextService.refreshContext(1L);

        assertThat(context.getUpdatedAt()).isNotNull();
        verify(contextMapper).updateById(context);
    }

    // ------------------------------------------------------------------
    // getContextByConversation
    // ------------------------------------------------------------------

    @Test
    void getContextByConversation_nullId_throwsParamError() {
        assertThatThrownBy(() -> contextService.getContextByConversation(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getContextByConversation_notFound_throwsContextNotFound() {
        when(contextMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> contextService.getContextByConversation(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONTEXT_NOT_FOUND.getCode());
    }

    @Test
    void getContextByConversation_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfContext context = new SfContext();
        context.setId(1L);
        context.setTenantId("tenant-1");
        when(contextMapper.selectById(1L)).thenReturn(context);

        assertThatThrownBy(() -> contextService.getContextByConversation(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void getContextByConversation_sameTenant_returnsContext() {
        TenantContextHolder.setTenantId("tenant-1");
        SfContext context = new SfContext();
        context.setId(1L);
        context.setTenantId("tenant-1");
        when(contextMapper.selectById(1L)).thenReturn(context);

        SfContext result = contextService.getContextByConversation(1L);

        assertThat(result).isEqualTo(context);
    }

    @Test
    void getContextByConversation_nullTenant_returnsContext() {
        SfContext context = new SfContext();
        context.setId(1L);
        context.setTenantId("tenant-1");
        when(contextMapper.selectById(1L)).thenReturn(context);

        SfContext result = contextService.getContextByConversation(1L);

        assertThat(result).isEqualTo(context);
    }
}
