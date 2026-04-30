package com.schemaplexai.model.entity.observability;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_observability_span")
public class ObservabilitySpan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String spanId;
    private String traceId;
    private String parentSpanId;
    private String name;
    private String type;
    private Long startTime;
    private Long endTime;
    private String input;
    private String output;
    private String metadata;
    private String status;
    private String model;
    private String modelParameters;
    private String usageDetails;
    private String costDetails;
    private String promptName;
    private String promptVersion;
}
