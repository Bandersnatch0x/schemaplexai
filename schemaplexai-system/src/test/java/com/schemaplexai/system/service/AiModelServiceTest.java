package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.system.entity.SfAiModel;
import com.schemaplexai.system.mapper.SfAiModelMapper;
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
class AiModelServiceTest {

    @Mock
    private SfAiModelMapper aiModelMapper;

    private AiModelService aiModelService;

    @BeforeEach
    void setUp() {
        aiModelService = new AiModelService();
        ReflectionTestUtils.setField(aiModelService, "baseMapper", aiModelMapper);
    }

    @Test
    void getById_returnsModel() {
        SfAiModel model = new SfAiModel();
        model.setId(1L);
        model.setName("GPT-4");
        when(aiModelMapper.selectById(1L)).thenReturn(model);

        SfAiModel result = aiModelService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("GPT-4");
    }

    @Test
    void getById_returnsNullWhenNotFound() {
        when(aiModelMapper.selectById(1L)).thenReturn(null);

        SfAiModel result = aiModelService.getById(1L);

        assertThat(result).isNull();
    }

    @Test
    void save_success_returnsTrue() {
        SfAiModel model = new SfAiModel();
        model.setName("GPT-4");
        when(aiModelMapper.insert(any(SfAiModel.class))).thenReturn(1);

        boolean result = aiModelService.save(model);

        assertThat(result).isTrue();
        verify(aiModelMapper).insert(model);
    }

    @Test
    void updateById_success_returnsTrue() {
        SfAiModel model = new SfAiModel();
        model.setId(1L);
        model.setName("GPT-4o");
        when(aiModelMapper.updateById(any(SfAiModel.class))).thenReturn(1);

        boolean result = aiModelService.updateById(model);

        assertThat(result).isTrue();
        verify(aiModelMapper).updateById(model);
    }

    @Test
    void page_returnsPageResult() {
        SfAiModel model = new SfAiModel();
        model.setId(1L);
        model.setName("GPT-4");
        Page<SfAiModel> page = new Page<>();
        page.setRecords(List.of(model));
        page.setTotal(1);
        when(aiModelMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Page<SfAiModel> result = aiModelService.page(new Page<>(1, 10));

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void list_returnsAllModels() {
        SfAiModel model1 = new SfAiModel();
        model1.setId(1L);
        SfAiModel model2 = new SfAiModel();
        model2.setId(2L);
        when(aiModelMapper.selectList(any())).thenReturn(List.of(model1, model2));

        List<SfAiModel> result = aiModelService.list();

        assertThat(result).hasSize(2);
    }
}
