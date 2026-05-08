package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/deliveries")
@RequiredArgsConstructor
@Tag(name = "交付管理", description = "交付记录创建、跟踪与确认接口")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @Operation(summary = "创建交付记录")
    public Result<Long> create(@RequestBody SfDeliveryRecord deliveryRecord) {
        deliveryService.save(deliveryRecord);
        return Result.success(deliveryRecord.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新交付记录")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfDeliveryRecord deliveryRecord) {
        deliveryRecord.setId(id);
        return Result.success(deliveryService.updateById(deliveryRecord));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除交付记录")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(deliveryService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取交付记录")
    public Result<SfDeliveryRecord> get(@PathVariable Long id) {
        SfDeliveryRecord deliveryRecord = deliveryService.getById(id);
        if (deliveryRecord == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(deliveryRecord);
    }

    @GetMapping
    @Operation(summary = "列出所有交付记录")
    public Result<List<SfDeliveryRecord>> list() {
        return Result.success(deliveryService.list());
    }

    @PostMapping("/create")
    @Operation(summary = "创建交付")
    public Result<SfDeliveryRecord> createDelivery(@RequestParam Long artifactId,
                                                   @RequestParam String deliveryType,
                                                   @RequestParam String recipient) {
        return Result.success(deliveryService.createDelivery(artifactId, deliveryType, recipient));
    }

    @PostMapping("/{id}/track")
    @Operation(summary = "跟踪交付状态")
    public Result<SfDeliveryRecord> trackDelivery(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(deliveryService.trackDelivery(id, status));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认交付")
    public Result<SfDeliveryRecord> confirmDelivery(@PathVariable Long id) {
        return Result.success(deliveryService.confirmDelivery(id));
    }

    @GetMapping("/by-status")
    @Operation(summary = "根据状态列出交付记录")
    public Result<List<SfDeliveryRecord>> listByStatus(@RequestParam Integer status) {
        return Result.success(deliveryService.listByStatus(status));
    }
}
