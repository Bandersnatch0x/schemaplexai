package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfSkill;
import com.schemaplexai.integration.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSkill skill) {
        skillService.save(skill);
        return Result.success(skill.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSkill skill) {
        skill.setId(id);
        return Result.success(skillService.updateById(skill));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(skillService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSkill> get(@PathVariable Long id) {
        SfSkill skill = skillService.getById(id);
        if (skill == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(skill);
    }

    @GetMapping
    public Result<List<SfSkill>> list() {
        return Result.success(skillService.list());
    }
}
