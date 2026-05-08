package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfIntegration;
import com.schemaplexai.integration.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/integrations")
@RequiredArgsConstructor
@Tag(name = "Integration Management", description = "Third-party integrations (GitHub, GitLab, Jenkins, webhooks)")
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping
    @Operation(summary = "Create integration")
    public Result<Long> create(@RequestBody SfIntegration integration) {
        integrationService.save(integration);
        return Result.success(integration.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update integration")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfIntegration integration) {
        integration.setId(id);
        return Result.success(integrationService.updateById(integration));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete integration")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(integrationService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get integration by id")
    public Result<SfIntegration> get(@PathVariable Long id) {
        SfIntegration integration = integrationService.getById(id);
        if (integration == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(integration);
    }

    @GetMapping
    @Operation(summary = "List all integrations")
    public Result<List<SfIntegration>> list() {
        return Result.success(integrationService.list());
    }
}
