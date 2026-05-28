package com.schemaplexai.task.scheduling;

import com.schemaplexai.task.service.ChatMessageArchiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageArchiveJobTest {

    @Mock
    private ChatMessageArchiveService archiveService;

    @InjectMocks
    private ChatMessageArchiveJob job;

    @Test
    void run_delegatesToArchiveServiceWithDefaultRetention() {
        assertThatNoException().isThrownBy(() -> job.run());

        verify(archiveService).archiveExpiredMessages(Duration.ofDays(90));
    }

    @Test
    void run_archiveThrowsException_propagates() {
        when(archiveService.archiveExpiredMessages(Duration.ofDays(90)))
                .thenThrow(new RuntimeException("archive failed"));

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("archive failed");
    }
}
