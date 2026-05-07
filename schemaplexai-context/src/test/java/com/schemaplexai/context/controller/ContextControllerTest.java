package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private KnowledgeDocService knowledgeDocService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ContextService contextService;

    @Mock
    private ContextSnapshotService contextSnapshotService;

    @InjectMocks
    private RagController ragController;

    @InjectMocks
    private KnowledgeDocController knowledgeDocController;

    @InjectMocks
    private WorkspaceController workspaceController;

    @InjectMocks
    private ContextController contextController;

    @InjectMocks
    private ContextSnapshotController contextSnapshotController;

    private SfWorkspace workspace;
    private SfContext context;
    private SfKnowledgeDoc doc;
    private SfContextSnapshot snapshot;

    @BeforeEach
    void setUp() {
        workspace = new SfWorkspace();
        workspace.setId(1L);
        workspace.setName("Default");

        context = new SfContext();
        context.setId(1L);
        context.setName("Test Context");

        doc = new SfKnowledgeDoc();
        doc.setId(1L);
        doc.setTitle("Doc");

        snapshot = new SfContextSnapshot();
        snapshot.setId(1L);
        snapshot.setContextId(1L);
    }

    // ========== RagController ==========

    @Test
    void ragRetrieve_returnsResults() {
        when(ragService.retrieve("query", 5)).thenReturn(List.of("chunk1", "chunk2"));

        RagController.RetrieveRequest request = new RagController.RetrieveRequest();
        request.setQuery("query");
        request.setTopK(5);

        Result<List<String>> result = ragController.retrieve(request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
    }

    // ========== KnowledgeDocController ==========

    @Test
    void docCreate_returnsId() {
        doNothing().when(knowledgeDocService).uploadAndVectorize(any());

        Result<Long> result = knowledgeDocController.create(doc);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void docUpdate_returnsBoolean() {
        when(knowledgeDocService.updateById(any())).thenReturn(true);

        Result<Boolean> result = knowledgeDocController.update(1L, doc);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void docDelete_returnsBoolean() {
        when(knowledgeDocService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = knowledgeDocController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void docGet_found() {
        when(knowledgeDocService.getById(1L)).thenReturn(doc);

        Result<SfKnowledgeDoc> result = knowledgeDocController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTitle()).isEqualTo("Doc");
    }

    @Test
    void docGet_notFound() {
        when(knowledgeDocService.getById(1L)).thenReturn(null);

        Result<SfKnowledgeDoc> result = knowledgeDocController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    // ========== WorkspaceController ==========

    @Test
    void workspaceCreate_returnsId() {
        when(workspaceService.save(any())).thenReturn(true);

        Result<Long> result = workspaceController.create(workspace);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void workspaceUpdate_returnsBoolean() {
        when(workspaceService.updateById(any())).thenReturn(true);

        Result<Boolean> result = workspaceController.update(1L, workspace);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void workspaceDelete_returnsBoolean() {
        when(workspaceService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = workspaceController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void workspaceGet_found() {
        when(workspaceService.getById(1L)).thenReturn(workspace);

        Result<SfWorkspace> result = workspaceController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Default");
    }

    @Test
    void workspaceGet_notFound() {
        when(workspaceService.getById(1L)).thenReturn(null);

        Result<SfWorkspace> result = workspaceController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void workspaceCreateDefault() {
        when(workspaceService.createDefaultWorkspace("t1")).thenReturn(workspace);

        Result<SfWorkspace> result = workspaceController.createDefaultWorkspace("t1");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void workspaceValidateAccess() {
        doNothing().when(workspaceService).validateWorkspaceAccess(1L);

        Result<Void> result = workspaceController.validateWorkspaceAccess(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void workspaceListByTenant() {
        when(workspaceService.listWorkspacesByTenant("t1")).thenReturn(List.of(workspace));

        Result<List<SfWorkspace>> result = workspaceController.listWorkspacesByTenant("t1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void workspaceArchive() {
        doNothing().when(workspaceService).archiveWorkspace(1L);

        Result<Void> result = workspaceController.archiveWorkspace(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    // ========== ContextController ==========

    @Test
    void contextCreate_returnsId() {
        when(contextService.save(any())).thenReturn(true);

        Result<Long> result = contextController.create(context);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void contextUpdate_returnsBoolean() {
        when(contextService.updateById(any())).thenReturn(true);

        Result<Boolean> result = contextController.update(1L, context);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void contextDelete_returnsBoolean() {
        when(contextService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = contextController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void contextGet_found() {
        when(contextService.getById(1L)).thenReturn(context);

        Result<SfContext> result = contextController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Test Context");
    }

    @Test
    void contextGet_notFound() {
        when(contextService.getById(1L)).thenReturn(null);

        Result<SfContext> result = contextController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void contextIngest() {
        when(contextService.ingestContext(1L, "name", "type")).thenReturn(context);

        Result<SfContext> result = contextController.ingestContext(1L, "name", "type");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void contextSearch() {
        when(contextService.searchContext("key", "type")).thenReturn(List.of(context));

        Result<List<SfContext>> result = contextController.searchContext("key", "type");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void contextRefresh() {
        doNothing().when(contextService).refreshContext(1L);

        Result<Void> result = contextController.refreshContext(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void contextByConversation_found() {
        when(contextService.getContextByConversation(1L)).thenReturn(context);

        Result<SfContext> result = contextController.getContextByConversation(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void contextByConversation_notFound() {
        when(contextService.getContextByConversation(1L)).thenReturn(null);

        Result<SfContext> result = contextController.getContextByConversation(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    // ========== ContextSnapshotController ==========

    @Test
    void snapshotCreate_returnsId() {
        when(contextSnapshotService.save(any())).thenReturn(true);

        Result<Long> result = contextSnapshotController.create(snapshot);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void snapshotUpdate_returnsBoolean() {
        when(contextSnapshotService.updateById(any())).thenReturn(true);

        Result<Boolean> result = contextSnapshotController.update(1L, snapshot);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void snapshotDelete_returnsBoolean() {
        when(contextSnapshotService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = contextSnapshotController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void snapshotGet_found() {
        when(contextSnapshotService.getById(1L)).thenReturn(snapshot);

        Result<SfContextSnapshot> result = contextSnapshotController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getContextId()).isEqualTo(1L);
    }

    @Test
    void snapshotGet_notFound() {
        when(contextSnapshotService.getById(1L)).thenReturn(null);

        Result<SfContextSnapshot> result = contextSnapshotController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void snapshotCreateSnapshot() {
        when(contextSnapshotService.createSnapshot(1L, "json")).thenReturn(snapshot);

        Result<SfContextSnapshot> result = contextSnapshotController.createSnapshot(1L, "json");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void snapshotRestore() {
        when(contextSnapshotService.restoreFromSnapshot(1L)).thenReturn("restored");

        Result<String> result = contextSnapshotController.restoreFromSnapshot(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("restored");
    }

    @Test
    void snapshotListByContext() {
        when(contextSnapshotService.listSnapshotsByContext(1L)).thenReturn(List.of(snapshot));

        Result<List<SfContextSnapshot>> result = contextSnapshotController.listSnapshotsByContext(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void snapshotCompare() {
        when(contextSnapshotService.compareSnapshots(1L, 2L)).thenReturn("diff");

        Result<String> result = contextSnapshotController.compareSnapshots(1L, 2L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("diff");
    }
}
