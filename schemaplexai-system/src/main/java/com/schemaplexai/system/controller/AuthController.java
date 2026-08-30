package com.schemaplexai.system.controller;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.system.dto.ChangePasswordRequest;
import com.schemaplexai.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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
        // Gateway strips X-Tenant-Id on whitelisted paths (/auth/**),
        // so accept tenantId from body as fallback
        String tenantId = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        if (!StringUtils.hasText(tenantId)) {
            tenantId = params.get("tenantId");
        }
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

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body,
                                       HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (!StringUtils.hasText(userIdStr)) {
            return Result.error(com.schemaplexai.common.result.ResultCode.UNAUTHORIZED);
        }
        Long userId = Long.parseLong(userIdStr);
        authService.changePassword(userId, body.getOldPassword(), body.getNewPassword());
        return Result.success();
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return null;
        }
        return authorization.substring(CommonConstants.TOKEN_PREFIX.length());
    }
}
