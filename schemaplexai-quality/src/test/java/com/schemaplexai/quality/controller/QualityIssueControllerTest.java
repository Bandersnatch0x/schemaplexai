package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.service.QualityIssueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityIssueControllerTest {

    @Mock
    private QualityIssueService qualityIssueService;

    @InjectMocks
    private QualityIssueController qualityIssueController;

    @Test
    void create_returnsId() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);

        Result<Long> result = qualityIssueController.create(issue);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
        verify(qualityIssueService).save(issue);
    }

    @Test
    void update_returnsBoolean() {
        SfQualityIssue issue = new SfQualityIssue();
        when(qualityIssueService.updateById(any())).thenReturn(true);

        Result<Boolean> result = qualityIssueController.update(1L, issue);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(issue.getId()).isEqualTo(1L);
        verify(qualityIssueService).updateById(issue);
    }

    @Test
    void update_returnsFalse_whenServiceFails() {
        SfQualityIssue issue = new SfQualityIssue();
        when(qualityIssueService.updateById(any())).thenReturn(false);

        Result<Boolean> result = qualityIssueController.update(1L, issue);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void delete_returnsBoolean() {
        when(qualityIssueService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = qualityIssueController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(qualityIssueService).removeById(1L);
    }

    @Test
    void delete_returnsFalse_whenServiceFails() {
        when(qualityIssueService.removeById(1L)).thenReturn(false);

        Result<Boolean> result = qualityIssueController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void get_found() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        when(qualityIssueService.getById(1L)).thenReturn(issue);

        Result<SfQualityIssue> result = qualityIssueController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(issue);
    }

    @Test
    void get_notFound() {
        when(qualityIssueService.getById(1L)).thenReturn(null);

        Result<SfQualityIssue> result = qualityIssueController.get(1L);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void list_returnsIssues() {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setId(1L);
        when(qualityIssueService.list()).thenReturn(List.of(issue));

        Result<List<SfQualityIssue>> result = qualityIssueController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void list_returnsEmptyList() {
        when(qualityIssueService.list()).thenReturn(Collections.emptyList());

        Result<List<SfQualityIssue>> result = qualityIssueController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
