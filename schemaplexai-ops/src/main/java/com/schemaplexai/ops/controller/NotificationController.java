package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public Result<Long> create(@RequestBody SfNotification notification) {
        notificationService.save(notification);
        return Result.success(notification.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfNotification notification) {
        notification.setId(id);
        return Result.success(notificationService.updateById(notification));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(notificationService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfNotification> get(@PathVariable Long id) {
        SfNotification notification = notificationService.getById(id);
        if (notification == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(notification);
    }

    @GetMapping
    public Result<List<SfNotification>> list() {
        return Result.success(notificationService.list());
    }

    @PostMapping("/send")
    public Result<SfNotification> sendNotification(@RequestParam Long userId,
                                                   @RequestParam String type,
                                                   @RequestParam String title,
                                                   @RequestParam String content) {
        return Result.success(notificationService.sendNotification(userId, type, title, content));
    }

    @PostMapping("/{id}/mark-read")
    public Result<SfNotification> markAsRead(@PathVariable Long id) {
        return Result.success(notificationService.markAsRead(id));
    }

    @GetMapping("/unread")
    public Result<List<SfNotification>> listUnread(@RequestParam Long userId) {
        return Result.success(notificationService.listUnread(userId));
    }

    @PostMapping("/batch-mark-read")
    public Result<Integer> batchMarkAsRead(@RequestBody List<Long> notificationIds) {
        return Result.success(notificationService.batchMarkAsRead(notificationIds));
    }
}
