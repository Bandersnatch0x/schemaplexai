package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_prompt_version")
public class SfPromptVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotNull
    private Long configId;

    @NotNull
    private Long agentId;

    private Integer version;

    @NotNull
    @Size(max = 5000)
    private String content;

    @NotNull
    @Size(max = 100)
    private String label;

    @Size(max = 500)
    private String changeNote;
}
