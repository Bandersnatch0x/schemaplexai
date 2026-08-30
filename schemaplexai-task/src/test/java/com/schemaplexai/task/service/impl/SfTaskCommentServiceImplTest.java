package com.schemaplexai.task.service.impl;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.entity.SfTaskComment;
import com.schemaplexai.task.mapper.SfTaskCommentMapper;
import com.schemaplexai.task.mapper.SfTaskMapper;
import com.schemaplexai.task.mapper.SfUserLookupMapper;
import com.schemaplexai.task.vo.TaskCommentVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task comment service (SfTaskCommentServiceImpl) tests")
class SfTaskCommentServiceImplTest {

    @Mock
    private SfTaskCommentMapper commentMapper;

    @Mock
    private SfTaskMapper taskMapper;

    @Mock
    private SfUserLookupMapper userLookupMapper;

    private SfTaskCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        // Mockito stops at constructor injection and never fills ServiceImpl's
        // inherited baseMapper field, so wire it explicitly.
        commentService = new SfTaskCommentServiceImpl(taskMapper, userLookupMapper);
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("listComments returns the task comments oldest first")
    void listComments_returnsCommentsOldestFirst() {
        when(taskMapper.selectById(3L)).thenReturn(task(3L));
        SfTaskComment older = comment(1L, 3L, "first", LocalDateTime.of(2026, 8, 1, 10, 0));
        SfTaskComment newer = comment(2L, 3L, "second", LocalDateTime.of(2026, 8, 2, 10, 0));
        when(commentMapper.selectList(any())).thenReturn(List.of(older, newer));

        List<TaskCommentVO> result = commentService.listComments(3L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("first");
        assertThat(result.get(0).getTaskId()).isEqualTo("3");
        assertThat(result.get(1).getId()).isEqualTo("2");
    }

    @Test
    @DisplayName("listComments fails with NOT_FOUND for a missing task")
    void listComments_missingTaskFailsWithNotFound() {
        when(taskMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.listComments(404L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
        verify(commentMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("addComment stores content and resolves the author display name")
    void addComment_storesContentAndResolvesAuthorName() {
        TenantContextHolder.setTenantId("1");
        when(taskMapper.selectById(3L)).thenReturn(task(3L));
        when(userLookupMapper.findUsernameById(7L, 1L)).thenReturn("alice");
        when(commentMapper.insert(any(SfTaskComment.class))).thenAnswer(invocation -> {
            SfTaskComment entity = invocation.getArgument(0);
            entity.setId(42L);
            entity.setCreatedAt(LocalDateTime.of(2026, 8, 30, 9, 0));
            return 1;
        });
        when(commentMapper.selectById(42L)).thenAnswer(invocation -> comment(42L, 3L, "lgtm", null));

        TaskCommentVO vo = commentService.addComment(3L, " lgtm ", 7L);

        ArgumentCaptor<SfTaskComment> captor = ArgumentCaptor.forClass(SfTaskComment.class);
        verify(commentMapper).insert(captor.capture());
        SfTaskComment inserted = captor.getValue();
        assertThat(inserted.getTaskId()).isEqualTo(3L);
        assertThat(inserted.getContent()).isEqualTo("lgtm");
        assertThat(inserted.getAuthorId()).isEqualTo(7L);
        assertThat(inserted.getAuthorName()).isEqualTo("alice");
        assertThat(inserted.getTenantId()).isEqualTo("1");
        assertThat(vo.getId()).isEqualTo("42");
        assertThat(vo.getAuthorName()).isNotNull();
    }

    @Test
    @DisplayName("addComment tolerates an unresolvable author display name")
    void addComment_toleratesUnresolvableAuthorName() {
        TenantContextHolder.setTenantId("1");
        when(taskMapper.selectById(3L)).thenReturn(task(3L));
        when(userLookupMapper.findUsernameById(9L, 1L)).thenReturn(null);
        when(commentMapper.insert(any(SfTaskComment.class))).thenAnswer(invocation -> {
            SfTaskComment entity = invocation.getArgument(0);
            entity.setId(43L);
            return 1;
        });
        SfTaskComment stored = comment(43L, 3L, "note", null);
        stored.setAuthorId(9L);
        stored.setAuthorName(null);
        when(commentMapper.selectById(43L)).thenReturn(stored);

        TaskCommentVO vo = commentService.addComment(3L, "note", 9L);

        assertThat(vo.getAuthorId()).isEqualTo("9");
        assertThat(vo.getAuthorName()).isNull();
    }

    @Test
    @DisplayName("addComment rejects blank content and missing author")
    void addComment_rejectsBlankContentAndMissingAuthor() {
        when(taskMapper.selectById(3L)).thenReturn(task(3L));

        assertThatThrownBy(() -> commentService.addComment(3L, "   ", 7L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());

        assertThatThrownBy(() -> commentService.addComment(3L, "content", null))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.UNAUTHORIZED.getCode());
        verify(commentMapper, never()).insert(any(SfTaskComment.class));
    }

    @Test
    @DisplayName("addComment fails with NOT_FOUND for a missing task")
    void addComment_missingTaskFailsWithNotFound() {
        when(taskMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.addComment(404L, "content", 7L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
    }

    private SfTask task(Long id) {
        SfTask task = new SfTask();
        task.setId(id);
        task.setTenantId("1");
        task.setTitle("task-" + id);
        return task;
    }

    private SfTaskComment comment(Long id, Long taskId, String content, LocalDateTime createdAt) {
        SfTaskComment comment = new SfTaskComment();
        comment.setId(id);
        comment.setTaskId(taskId);
        comment.setContent(content);
        comment.setAuthorId(7L);
        comment.setAuthorName("alice");
        comment.setCreatedAt(createdAt == null ? LocalDateTime.of(2026, 8, 30, 9, 0) : createdAt);
        return comment;
    }
}
