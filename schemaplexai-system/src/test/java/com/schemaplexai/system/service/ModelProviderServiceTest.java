package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.system.entity.SfModelProvider;
import com.schemaplexai.system.mapper.SfModelProviderMapper;
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
class ModelProviderServiceTest {

    @Mock
    private SfModelProviderMapper modelProviderMapper;

    private ModelProviderService modelProviderService;

    @BeforeEach
    void setUp() {
        modelProviderService = new ModelProviderService();
        ReflectionTestUtils.setField(modelProviderService, "baseMapper", modelProviderMapper);
    }

    @Test
    void getById_returnsProvider() {
        SfModelProvider provider = new SfModelProvider();
        provider.setId(1L);
        provider.setName("OpenAI");
        when(modelProviderMapper.selectById(1L)).thenReturn(provider);

        SfModelProvider result = modelProviderService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("OpenAI");
    }

    @Test
    void getById_returnsNullWhenNotFound() {
        when(modelProviderMapper.selectById(1L)).thenReturn(null);

        SfModelProvider result = modelProviderService.getById(1L);

        assertThat(result).isNull();
    }

    @Test
    void save_success_returnsTrue() {
        SfModelProvider provider = new SfModelProvider();
        provider.setName("OpenAI");
        provider.setCode("openai");
        when(modelProviderMapper.insert(any(SfModelProvider.class))).thenReturn(1);

        boolean result = modelProviderService.save(provider);

        assertThat(result).isTrue();
        verify(modelProviderMapper).insert(provider);
    }

    @Test
    void updateById_success_returnsTrue() {
        SfModelProvider provider = new SfModelProvider();
        provider.setId(1L);
        provider.setName("OpenAI Updated");
        when(modelProviderMapper.updateById(any(SfModelProvider.class))).thenReturn(1);

        boolean result = modelProviderService.updateById(provider);

        assertThat(result).isTrue();
        verify(modelProviderMapper).updateById(provider);
    }

    @Test
    void page_returnsPageResult() {
        SfModelProvider provider = new SfModelProvider();
        provider.setId(1L);
        provider.setName("OpenAI");
        Page<SfModelProvider> page = new Page<>();
        page.setRecords(List.of(provider));
        page.setTotal(1);
        when(modelProviderMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Page<SfModelProvider> result = modelProviderService.page(new Page<>(1, 10));

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void list_returnsAllProviders() {
        SfModelProvider provider1 = new SfModelProvider();
        provider1.setId(1L);
        SfModelProvider provider2 = new SfModelProvider();
        provider2.setId(2L);
        when(modelProviderMapper.selectList(any())).thenReturn(List.of(provider1, provider2));

        List<SfModelProvider> result = modelProviderService.list();

        assertThat(result).hasSize(2);
    }
}
