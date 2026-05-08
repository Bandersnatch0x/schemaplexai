package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get skill by id")
    public Result<SfSkill> get(@PathVariable Long id) {
        SfSkill skill = skillService.getById(id);
        if (skill == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(skill);
    }

    @GetMapping
    @Operation(summary = "List all skills")
    public Result<List<SfSkill>> list() {
        return Result.success(skillService.list());
    }
}
