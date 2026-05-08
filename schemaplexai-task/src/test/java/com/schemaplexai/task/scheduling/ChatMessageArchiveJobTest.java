package com.schemaplexai.task.scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class ChatMessageArchiveJobTest {

    @InjectMocks
    private ChatMessageArchiveJob job;

    @Test
    void run_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> job.run());
    }
}
