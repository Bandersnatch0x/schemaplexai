package com.schemaplexai.task.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ChatMessageArchiveMapper {

    @Insert("""
            INSERT INTO sf_chat_message_archive (
                id, tenant_id, conversation_id, turn_index, role, content, tool_calls, token_count, created_at
            )
            SELECT
                message.id,
                message.tenant_id,
                message.conversation_id,
                message.turn_index,
                message.role,
                message.content,
                message.tool_calls,
                message.token_count,
                message.created_at
            FROM sf_chat_message message
            WHERE message.created_at < #{cutoff}
              AND NOT EXISTS (
                  SELECT 1
                  FROM sf_chat_message_archive archive
                  WHERE archive.id = message.id
                    AND archive.conversation_id = message.conversation_id
              )
            """)
    int insertExpiredMessages(@Param("cutoff") LocalDateTime cutoff);

    @Delete("""
            DELETE FROM sf_chat_message message
            WHERE message.created_at < #{cutoff}
              AND EXISTS (
                  SELECT 1
                  FROM sf_chat_message_archive archive
                  WHERE archive.id = message.id
                    AND archive.conversation_id = message.conversation_id
              )
            """)
    int deleteArchivedMessages(@Param("cutoff") LocalDateTime cutoff);
}
