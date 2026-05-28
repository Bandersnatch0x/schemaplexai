package com.schemaplexai.system.controller;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证授权")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String username = params.get("username");
        String password = params.get("password");
        String tenantId = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        return Result.success(authService.login(username, password, tenantId));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<Map<String, String>> refresh(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        return Result.success(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        String token = resolveBearerToken(request.getHeader(CommonConstants.HEADER_AUTHORIZATION));
        authService.logout(userId, token);
        return Result.success();
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return null;
        }
        return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
    }
}
