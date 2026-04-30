package com.schemaplexai.web.controller.notification;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.schemaplexai.web.controller.BaseController;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.notification.NotificationService;
import com.schemaplexai.web.vo.notification.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知管理", description = "系统通知相关接口")
@RestController
@RequestMapping("/web/notification")
@RequiredArgsConstructor
@Validated
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @Operation(summary = "查询通知列表", description = "分页查询当前用户的通知列表，支持已读/未读筛选")
    @GetMapping("/page")
    public Result<IPage<NotificationVO>> page(
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，默认20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "已读状态筛选") @RequestParam(required = false) Boolean read) {
        return success(notificationService.pageQuery(Long.valueOf(userId), page, size, read));
    }

    @Operation(summary = "标记已读", description = "将单条通知标记为已读")
    @PutMapping("/{id}/read")
    public Result<Boolean> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "通知ID") @PathVariable Long id) {
        return success(notificationService.markAsRead(id, Long.valueOf(userId)));
    }

    @Operation(summary = "全部已读", description = "将当前用户所有未读通知标记为已读")
    @PutMapping("/read-all")
    public Result<Integer> markAllAsRead(
            @RequestHeader("X-User-Id") String userId) {
        return success(notificationService.markAllAsRead(Long.valueOf(userId)));
    }
}
