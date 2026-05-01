package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.mapper.PromptVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptVersionServiceTest {

    @Mock
    private PromptVersionMapper promptVersionMapper;

    @InjectMocks
    private PromptVersionServiceImpl promptVersionService;

    @Test
    void shouldCreateVersionWithIncrementedNumber() {
        SfPromptVersion latest = new SfPromptVersion();
        latest.setVersion(2);
        when(promptVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latest);

        SfPromptVersion result = promptVersionService.createVersion(
                1L, 10L, "Content v3", "review", "Ready for review");

        assertThat(result.getVersion()).isEqualTo(3);
        assertThat(result.getConfigId()).isEqualTo(1L);
        assertThat(result.getAgentId()).isEqualTo(10L);
        assertThat(result.getContent()).isEqualTo("Content v3");
        assertThat(result.getLabel()).isEqualTo("review");
        assertThat(result.getChangeNote()).isEqualTo("Ready for review");

        verify(promptVersionMapper).insert(result);
    }

    @Test
    void shouldFindByLabel() {
        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(2L);
        pv.setLabel("production");
        pv.setContent("Production content");

        when(promptVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pv);

        Optional<SfPromptVersion> found = promptVersionService.getByLabel(2L, "production");

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Production content");
    }

    @Test
    void shouldReturnEmptyWhenLabelNotFound() {
        when(promptVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Optional<SfPromptVersion> found = promptVersionService.getByLabel(2L, "missing");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldListVersionsOrderedByVersionDesc() {
        SfPromptVersion v2 = new SfPromptVersion();
        v2.setVersion(2);
        SfPromptVersion v1 = new SfPromptVersion();
        v1.setVersion(1);

        when(promptVersionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(v2, v1));

        List<SfPromptVersion> versions = promptVersionService.listVersions(1L);

        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getVersion()).isEqualTo(2);
    }
}
