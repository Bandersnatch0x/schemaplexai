package com.schemaplexai.system.user.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private String username;
    private String tenantId;
}
