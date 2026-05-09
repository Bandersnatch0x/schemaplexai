package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.service.ContextSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextSnapshotControllerTest {

    @Mock
    private ContextSnapshotService contextSnapshotService;

    @InjectMocks
    private ContextSnapshotController controller;

    @Test
    void create_success() {
        SfContextSnapshot snapshot = new SfContextSnapshot();
        snapshot.setId(1L);
        when(contextSnapshotService.save(snapshot)).thenReturn(true);
        Result<Long> result = controller.create(snapshot);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_success() {
        SfContextSnapshot snapshot = new SfContextSnapshot();
        when(contextSnapshotService.updateById(snapshot)).thenReturn(true);
        Result<Boolean> result = controller.update(1L, snapshot);
        assertThat(result.getData()).isTrue();
        assertThat(snapshot.getId()).isEqualTo(1L);
    }

    @Test
    void delete_success() {
        when(contextSnapshotService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = controller.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        SfContextSnapshot snapshot = new SfContextSnapshot();
        when(contextSnapshotService.getById(1L)).thenReturn(snapshot);
        Result<SfContextSnapshot> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void get_notFound() {
        when(contextSnapshotService.getById(1L)).thenReturn(null);
        Result<SfContextSnapshot> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createSnapshot_success() {
        SfContextSnapshot snapshot = new SfContextSnapshot();
        when(contextSnapshotService.createSnapshot(1L, "{}")).thenReturn(snapshot);
        Result<SfContextSnapshot> result = controller.createSnapshot(1L, "{}");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void restoreFromSnapshot_success() {
        when(contextSnapshotService.restoreFromSnapshot(1L)).thenReturn("restored");
        Result<String> result = controller.restoreFromSnapshot(1L);
        assertThat(result.getData()).isEqualTo("restored");
    }

    @Test
    void listSnapshotsByContext_success() {
        when(contextSnapshotService.listSnapshotsByContext(1L)).thenReturn(List.of());
        Result<List<SfContextSnapshot>> result = controller.listSnapshotsByContext(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void compareSnapshots_success() {
        when(contextSnapshotService.compareSnapshots(1L, 2L)).thenReturn("diff");
        Result<String> result = controller.compareSnapshots(1L, 2L);
        assertThat(result.getData()).isEqualTo("diff");
    }
}
