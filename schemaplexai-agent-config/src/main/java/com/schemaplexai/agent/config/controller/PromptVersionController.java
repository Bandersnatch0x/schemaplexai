package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.service.PromptVersionService;
import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agent-config/prompt-versions")
@RequiredArgsConstructor
@Tag(name = "Prompt Version", description = "Prompt version management")
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @PostMapping
    @PreAuthorize("hasAuthority('agent:config:write')")
    @Operation(summary = "Create prompt version")
    public Result<SfPromptVersion> create(@Valid @RequestBody SfPromptVersion request) {
        SfPromptVersion pv = promptVersionService.createVersion(
            request.getConfigId(), request.getAgentId(),
            request.getContent(), request.getLabel(), request.getChangeNote());
        return Result.success(pv);
    }

    @GetMapping("/by-label")
    @Operation(summary = "Get prompt version by label")
    public Result<SfPromptVersion> getByLabel(@RequestParam Long configId,
                                               @RequestParam String label) {
        Optional<SfPromptVersion> pv = promptVersionService.getByLabel(configId, label);
        return pv.map(Result::success)
                 .orElse(Result.error("Prompt version not found"));
    }

    @GetMapping
    @Operation(summary = "List prompt versions by config ID")
    public Result<List<SfPromptVersion>> list(@RequestParam Long configId) {
        return Result.success(promptVersionService.listVersions(configId));
    }
}
