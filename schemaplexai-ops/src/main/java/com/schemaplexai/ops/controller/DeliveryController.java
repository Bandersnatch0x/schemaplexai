package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public Result<Long> create(@RequestBody SfDeliveryRecord deliveryRecord) {
        deliveryService.save(deliveryRecord);
        return Result.success(deliveryRecord.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfDeliveryRecord deliveryRecord) {
        deliveryRecord.setId(id);
        return Result.success(deliveryService.updateById(deliveryRecord));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(deliveryService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfDeliveryRecord> get(@PathVariable Long id) {
        SfDeliveryRecord deliveryRecord = deliveryService.getById(id);
        if (deliveryRecord == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(deliveryRecord);
    }

    @GetMapping
    public Result<List<SfDeliveryRecord>> list() {
        return Result.success(deliveryService.list());
    }

    @PostMapping("/create")
    public Result<SfDeliveryRecord> createDelivery(@RequestParam Long artifactId,
                                                   @RequestParam String deliveryType,
                                                   @RequestParam String recipient) {
        return Result.success(deliveryService.createDelivery(artifactId, deliveryType, recipient));
    }

    @PostMapping("/{id}/track")
    public Result<SfDeliveryRecord> trackDelivery(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(deliveryService.trackDelivery(id, status));
    }

    @PostMapping("/{id}/confirm")
    public Result<SfDeliveryRecord> confirmDelivery(@PathVariable Long id) {
        return Result.success(deliveryService.confirmDelivery(id));
    }

    @GetMapping("/by-status")
    public Result<List<SfDeliveryRecord>> listByStatus(@RequestParam Integer status) {
        return Result.success(deliveryService.listByStatus(status));
    }
}
