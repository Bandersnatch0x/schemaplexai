package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.service.PromptVersionService;
import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptVersionControllerTest {

    @Mock
    private PromptVersionService promptVersionService;

    @InjectMocks
    private PromptVersionController promptVersionController;

    @Test
    void create_returnsCreatedVersion() {
        SfPromptVersion request = new SfPromptVersion();
        request.setConfigId(1L);
        request.setAgentId(10L);
        request.setContent("Prompt content");
        request.setLabel("review");
        request.setChangeNote("Initial version");

        SfPromptVersion created = new SfPromptVersion();
        created.setId(1L);
        created.setVersion(1);
        created.setConfigId(1L);
        created.setContent("Prompt content");
        when(promptVersionService.createVersion(1L, 10L, "Prompt content", "review", "Initial version"))
                .thenReturn(created);

        Result<SfPromptVersion> result = promptVersionController.create(request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getVersion()).isEqualTo(1);
        assertThat(result.getData().getContent()).isEqualTo("Prompt content");
    }

    @Test
    void getByLabel_found() {
        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(1L);
        pv.setLabel("production");
        pv.setContent("Production prompt");
        when(promptVersionService.getByLabel(1L, "production")).thenReturn(Optional.of(pv));

        Result<SfPromptVersion> result = promptVersionController.getByLabel(1L, "production");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getContent()).isEqualTo("Production prompt");
    }

    @Test
    void getByLabel_notFound() {
        when(promptVersionService.getByLabel(1L, "missing")).thenReturn(Optional.empty());

        Result<SfPromptVersion> result = promptVersionController.getByLabel(1L, "missing");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("Prompt version not found");
    }

    @Test
    void list_returnsVersions() {
        SfPromptVersion v2 = new SfPromptVersion();
        v2.setVersion(2);
        v2.setContent("Version 2");
        SfPromptVersion v1 = new SfPromptVersion();
        v1.setVersion(1);
        v1.setContent("Version 1");
        when(promptVersionService.listVersions(1L)).thenReturn(List.of(v2, v1));

        Result<List<SfPromptVersion>> result = promptVersionController.list(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getVersion()).isEqualTo(2);
    }

    @Test
    void list_returnsEmptyList() {
        when(promptVersionService.listVersions(1L)).thenReturn(List.of());

        Result<List<SfPromptVersion>> result = promptVersionController.list(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
