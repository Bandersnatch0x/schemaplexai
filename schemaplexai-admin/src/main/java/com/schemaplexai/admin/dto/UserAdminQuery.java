package com.schemaplexai.admin.dto;

import com.schemaplexai.common.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAdminQuery extends PageParam {

    private String tenantId;
    private String username;
    private String email;
    private String phone;
    private Integer status;
}
