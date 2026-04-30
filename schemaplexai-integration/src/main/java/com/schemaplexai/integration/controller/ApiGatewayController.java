package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;
import com.schemaplexai.integration.service.ApiGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/api-gateways")
@RequiredArgsConstructor
public class ApiGatewayController {

    private final ApiGatewayService apiGatewayService;

    @PostMapping
    public Result<Long> create(@RequestBody SfApiGatewayConfig config) {
        apiGatewayService.save(config);
        return Result.success(config.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfApiGatewayConfig config) {
        config.setId(id);
        return Result.success(apiGatewayService.updateById(config));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(apiGatewayService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfApiGatewayConfig> get(@PathVariable Long id) {
        SfApiGatewayConfig config = apiGatewayService.getById(id);
        if (config == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(config);
    }

    @GetMapping
    public Result<List<SfApiGatewayConfig>> list() {
        return Result.success(apiGatewayService.list());
    }
}
