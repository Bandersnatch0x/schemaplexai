package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_eval_task")
public class SfEvalTask extends BaseEntity {

    private Long datasetId;
    private Long agentId;
    private Integer status;
    private String resultJson;
}
