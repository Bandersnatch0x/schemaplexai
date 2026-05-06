package com.schemaplexai.spec.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecTemplateMapper;
import com.schemaplexai.spec.service.impl.SpecTemplateServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecTemplateServiceImplTest {

    @Mock
    private SfSpecTemplateMapper specTemplateMapper;

    @Mock
    private SfSpecMapper specMapper;

    @InjectMocks
    private SpecTemplateServiceImpl specTemplateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specTemplateService, "baseMapper", specTemplateMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // applyTemplate
    // ------------------------------------------------------------------

    @Test
    void applyTemplate_templateNotFound_throwsNotFound() {
        when(specTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specTemplateService.applyTemplate(1L, null, "Title", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void applyTemplate_withSpecId_updatesExistingSpec() {
        SfSpecTemplate template = new SfSpecTemplate();
        template.setId(1L);
        template.setContent("template content");
        when(specTemplateMapper.selectById(1L)).thenReturn(template);

        SfSpec existingSpec = new SfSpec();
        existingSpec.setId(2L);
        existingSpec.setTitle("Old Title");
        when(specMapper.selectById(2L)).thenReturn(existingSpec);

        SfSpec result = specTemplateService.applyTemplate(1L, 2L, "New Title", "type");

        assertThat(result.getContent()).isEqualTo("template content");
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(specMapper).updateById(existingSpec);
        verify(specMapper, never()).insert(any(SfSpec.class));
    }

    @Test
    void applyTemplate_withSpecId_notFound_throwsSpecNotFound() {
        SfSpecTemplate template = new SfSpecTemplate();
        template.setId(1L);
        when(specTemplateMapper.selectById(1L)).thenReturn(template);
        when(specMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> specTemplateService.applyTemplate(1L, 2L, "Title", "type"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void applyTemplate_withoutSpecId_createsNewSpec() {
        SfSpecTemplate template = new SfSpecTemplate();
        template.setId(1L);
        template.setContent("template content");
        when(specTemplateMapper.selectById(1L)).thenReturn(template);

        SfSpec result = specTemplateService.applyTemplate(1L, null, "New Spec", "requirement");

        assertThat(result.getTitle()).isEqualTo("New Spec");
        assertThat(result.getType()).isEqualTo("requirement");
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getContent()).isEqualTo("template content");
        verify(specMapper).insert(any(SfSpec.class));
        verify(specMapper, never()).updateById(any(SfSpec.class));
    }

    // ------------------------------------------------------------------
    // getDefaultTemplate
    // ------------------------------------------------------------------

    @Test
    void getDefaultTemplate_noTemplate_returnsEmpty() {
        when(specTemplateMapper.selectOne(any())).thenReturn(null);

        Optional<SfSpecTemplate> result = specTemplateService.getDefaultTemplate("api");

        assertThat(result).isEmpty();
    }

    @Test
    void getDefaultTemplate_found_returnsTemplate() {
        SfSpecTemplate template = new SfSpecTemplate();
        template.setName("Default API");
        when(specTemplateMapper.selectOne(any())).thenReturn(template);

        Optional<SfSpecTemplate> result = specTemplateService.getDefaultTemplate("api");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Default API");
    }

    @Test
    void getDefaultTemplate_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        SfSpecTemplate template = new SfSpecTemplate();
        when(specTemplateMapper.selectOne(any())).thenReturn(template);

        Optional<SfSpecTemplate> result = specTemplateService.getDefaultTemplate("api");

        assertThat(result).isPresent();
    }

    // ------------------------------------------------------------------
    // listTemplatesByCategory
    // ------------------------------------------------------------------

    @Test
    void listTemplatesByCategory_returnsTemplates() {
        SfSpecTemplate t1 = new SfSpecTemplate();
        t1.setName("T1");
        SfSpecTemplate t2 = new SfSpecTemplate();
        t2.setName("T2");
        when(specTemplateMapper.selectList(any())).thenReturn(List.of(t1, t2));

        List<SfSpecTemplate> result = specTemplateService.listTemplatesByCategory("api");

        assertThat(result).hasSize(2);
    }

    @Test
    void listTemplatesByCategory_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(specTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfSpecTemplate> result = specTemplateService.listTemplatesByCategory("api");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // cloneTemplate
    // ------------------------------------------------------------------

    @Test
    void cloneTemplate_sourceNotFound_throwsNotFound() {
        when(specTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specTemplateService.cloneTemplate(1L, "Clone"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void cloneTemplate_success_createsClone() {
        SfSpecTemplate source = new SfSpecTemplate();
        source.setId(1L);
        source.setContent("source content");
        source.setCategory("api");
        when(specTemplateMapper.selectById(1L)).thenReturn(source);

        SfSpecTemplate result = specTemplateService.cloneTemplate(1L, "Cloned Template");

        assertThat(result.getName()).isEqualTo("Cloned Template");
        assertThat(result.getContent()).isEqualTo("source content");
        assertThat(result.getCategory()).isEqualTo("api");
        verify(specTemplateMapper).insert(any(SfSpecTemplate.class));
    }
}
