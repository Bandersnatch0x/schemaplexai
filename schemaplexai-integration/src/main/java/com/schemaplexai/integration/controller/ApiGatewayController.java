package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;
import com.schemaplexai.integration.service.ApiGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/api-gateways")
@RequiredArgsConstructor
@Tag(name = "API Gateway Management", description = "API gateway configuration and routing")
public class ApiGatewayController {

    private final ApiGatewayService apiGatewayService;

    @PostMapping
    @Operation(summary = "Create API gateway config")
    public Result<Long> create(@RequestBody SfApiGatewayConfig config) {
        apiGatewayService.save(config);
        return Result.success(config.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update API gateway config")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfApiGatewayConfig config) {
        config.setId(id);
        return Result.success(apiGatewayService.updateById(config));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete API gateway config")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(apiGatewayService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get API gateway config by id")
    public Result<SfApiGatewayConfig> get(@PathVariable Long id) {
        SfApiGatewayConfig config = apiGatewayService.getById(id);
        if (config == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(config);
    }

    @GetMapping
    @Operation(summary = "List all API gateway configs")
    public Result<List<SfApiGatewayConfig>> list() {
        return Result.success(apiGatewayService.list());
    }
}
