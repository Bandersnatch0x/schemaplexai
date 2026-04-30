package com.schemaplexai.model.entity.observability;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_observability_trace")
public class ObservabilityTrace extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String name;
    private String userId;
    private String sessionId;
    private String input;
    private String output;
    private String metadata;
    private String tags;
    private String version;
}
