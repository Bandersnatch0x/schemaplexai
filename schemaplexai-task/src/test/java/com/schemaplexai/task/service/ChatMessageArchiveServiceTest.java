package com.schemaplexai.task.service;

import com.schemaplexai.task.mapper.ChatMessageArchiveMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageArchiveServiceTest {

    @Mock
    private ChatMessageArchiveMapper archiveMapper;

    @InjectMocks
    private ChatMessageArchiveService archiveService;

    @Test
    void archiveExpiredMessages_movesExpiredRowsThenDeletesHotRows() {
        when(archiveMapper.insertExpiredMessages(any())).thenReturn(3);
        when(archiveMapper.deleteArchivedMessages(any())).thenReturn(3);

        int archived = archiveService.archiveExpiredMessages(Duration.ofDays(90));

        assertThat(archived).isEqualTo(3);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        var order = inOrder(archiveMapper);
        order.verify(archiveMapper).insertExpiredMessages(cutoffCaptor.capture());
        order.verify(archiveMapper).deleteArchivedMessages(cutoffCaptor.capture());

        assertThat(cutoffCaptor.getAllValues()).hasSize(2);
        assertThat(cutoffCaptor.getAllValues().get(1)).isEqualTo(cutoffCaptor.getAllValues().get(0));
        assertThat(cutoffCaptor.getValue()).isBefore(LocalDateTime.now().minusDays(89));
        assertThat(cutoffCaptor.getValue()).isAfter(LocalDateTime.now().minusDays(91));
    }

    @Test
    void archiveExpiredMessages_deletesHotRowsThatWereAlreadyArchived() {
        when(archiveMapper.insertExpiredMessages(any())).thenReturn(0);
        when(archiveMapper.deleteArchivedMessages(any())).thenReturn(2);

        int archived = archiveService.archiveExpiredMessages(Duration.ofDays(90));

        assertThat(archived).isEqualTo(2);

        var order = inOrder(archiveMapper);
        order.verify(archiveMapper).insertExpiredMessages(any());
        order.verify(archiveMapper).deleteArchivedMessages(any());
    }

    @Test
    void archiveExpiredMessages_rejectsNonPositiveRetention() {
        assertThatThrownBy(() -> archiveService.archiveExpiredMessages(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retention");

        verifyNoInteractions(archiveMapper);
    }
}
