package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_chat_message")
public class SfChatMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String conversationId;

    private Integer turnIndex;

    private String role;

    private String content;

    private String toolCalls;

    private Long tokenCount;
}
