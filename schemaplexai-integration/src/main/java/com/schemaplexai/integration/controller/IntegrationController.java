package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfIntegration;
import com.schemaplexai.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping
    public Result<Long> create(@RequestBody SfIntegration integration) {
        integrationService.save(integration);
        return Result.success(integration.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfIntegration integration) {
        integration.setId(id);
        return Result.success(integrationService.updateById(integration));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(integrationService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfIntegration> get(@PathVariable Long id) {
        SfIntegration integration = integrationService.getById(id);
        if (integration == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(integration);
    }

    @GetMapping
    public Result<List<SfIntegration>> list() {
        return Result.success(integrationService.list());
    }
}
