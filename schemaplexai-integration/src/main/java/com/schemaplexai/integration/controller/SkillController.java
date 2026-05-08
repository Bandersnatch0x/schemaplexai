package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.SkillContent;
import com.schemaplexai.integration.dto.SkillSummary;
import com.schemaplexai.integration.entity.SfSkill;
import com.schemaplexai.integration.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/skills")
@RequiredArgsConstructor
@Tag(name = "Skill Management", description = "Agent skill definitions and versioning")
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    @Operation(summary = "Create skill")
    public Result<Long> create(@RequestBody SfSkill skill) {
        skillService.save(skill);
        return Result.success(skill.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update skill")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSkill skill) {
        skill.setId(id);
        return Result.success(skillService.updateById(skill));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete skill")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(skillService.removeById(id));
    }

    /**
     * Get skill summary by ID (without content).
     * Use {@code GET /integration/skills/{id}/content} to fetch full content on demand.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get skill summary by id (without content)")
    public Result<SkillSummary> get(@PathVariable Long id) {
        SkillSummary summary = skillService.getSummaryById(id);
        if (summary == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(summary);
    }

    /**
     * List all skill summaries (without content).
     * Progressive disclosure: content is loaded on demand via /content endpoint.
     */
    @GetMapping
    @Operation(summary = "List all skill summaries (without content)")
    public Result<List<SkillSummary>> list() {
        return Result.success(skillService.listSummaries());
    }

    /**
     * Load skill content on demand.
     * Reduces token costs by separating metadata from content.
     */
    @GetMapping("/{id}/content")
    @Operation(summary = "Load skill content on demand")
    public Result<SkillContent> getContent(@PathVariable Long id) {
        SkillContent content = skillService.getContent(id);
        if (content == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(content);
    }
}
