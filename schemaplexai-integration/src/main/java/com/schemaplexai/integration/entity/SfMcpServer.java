package com.schemaplexai.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_mcp_server")
public class SfMcpServer extends BaseEntity {

    private String name;
    private String endpoint;
    private String transport;
    private Integer status;
}
