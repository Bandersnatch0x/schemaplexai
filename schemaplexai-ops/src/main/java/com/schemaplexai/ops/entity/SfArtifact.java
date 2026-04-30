package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_artifact")
public class SfArtifact extends BaseEntity {

    private String name;
    private String version;
    private String fileUrl;
    private String artifactType;
    private Integer status;
}
