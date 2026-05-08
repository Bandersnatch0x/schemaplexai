package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "通知发送、已读标记与批量处理接口")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "创建通知")
    public Result<Long> create(@RequestBody SfNotification notification) {
        notificationService.save(notification);
        return Result.success(notification.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新通知")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfNotification notification) {
        notification.setId(id);
        return Result.success(notificationService.updateById(notification));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(notificationService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取通知")
    public Result<SfNotification> get(@PathVariable Long id) {
        SfNotification notification = notificationService.getById(id);
        if (notification == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(notification);
    }

    @GetMapping
    @Operation(summary = "列出所有通知")
    public Result<List<SfNotification>> list() {
        return Result.success(notificationService.list());
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知")
    public Result<SfNotification> sendNotification(@RequestParam Long userId,
                                                   @RequestParam String type,
                                                   @RequestParam String title,
                                                   @RequestParam String content) {
        return Result.success(notificationService.sendNotification(userId, type, title, content));
    }

    @PostMapping("/{id}/mark-read")
    @Operation(summary = "标记通知为已读")
    public Result<SfNotification> markAsRead(@PathVariable Long id) {
        return Result.success(notificationService.markAsRead(id));
    }

    @GetMapping("/unread")
    @Operation(summary = "列出用户未读通知")
    public Result<List<SfNotification>> listUnread(@RequestParam Long userId) {
        return Result.success(notificationService.listUnread(userId));
    }

    @PostMapping("/batch-mark-read")
    @Operation(summary = "批量标记通知为已读")
    public Result<Integer> batchMarkAsRead(@RequestBody List<Long> notificationIds) {
        return Result.success(notificationService.batchMarkAsRead(notificationIds));
    }
}
