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
        String userId = request.getHeader("X-User-Id");
        authService.logout(userId);
        return Result.success();
    }
}
