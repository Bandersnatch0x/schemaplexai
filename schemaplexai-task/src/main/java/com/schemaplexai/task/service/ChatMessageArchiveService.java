package com.schemaplexai.task.service;

import com.schemaplexai.task.mapper.ChatMessageArchiveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageArchiveService {

    private final ChatMessageArchiveMapper archiveMapper;

    @Transactional
    public int archiveExpiredMessages(Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("chat message archive retention must be positive");
        }

        LocalDateTime cutoff = LocalDateTime.now().minus(retention);
        int inserted = archiveMapper.insertExpiredMessages(cutoff);
        int deleted = archiveMapper.deleteArchivedMessages(cutoff);
        if (inserted == 0 && deleted == 0) {
            log.info("[ChatMessageArchiveService] No chat messages older than {} to archive", cutoff);
            return 0;
        }

        log.info("[ChatMessageArchiveService] Archived {} chat messages and removed {} hot rows older than {}",
                inserted, deleted, cutoff);
        return deleted;
    }
}
