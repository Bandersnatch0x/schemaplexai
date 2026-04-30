package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.service.PromptVersionService;
import com.schemaplexai.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/agent-config/prompt-versions")
@RequiredArgsConstructor
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @PostMapping
    public Result<SfPromptVersion> create(@RequestBody SfPromptVersion request) {
        SfPromptVersion pv = promptVersionService.createVersion(
            request.getConfigId(), request.getAgentId(),
            request.getContent(), request.getLabel(), request.getChangeNote());
        return Result.success(pv);
    }

    @GetMapping("/by-label")
    public Result<SfPromptVersion> getByLabel(@RequestParam Long configId,
                                               @RequestParam String label) {
        Optional<SfPromptVersion> pv = promptVersionService.getByLabel(configId, label);
        return pv.map(Result::success)
                 .orElse(Result.error("Prompt version not found"));
    }

    @GetMapping
    public Result<List<SfPromptVersion>> list(@RequestParam Long configId) {
        return Result.success(promptVersionService.listVersions(configId));
    }
}
