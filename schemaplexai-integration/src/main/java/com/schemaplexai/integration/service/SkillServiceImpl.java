package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.SkillContent;
import com.schemaplexai.integration.dto.SkillSummary;
import com.schemaplexai.integration.entity.SfSkill;
import com.schemaplexai.integration.mapper.SkillMapper;
import com.schemaplexai.integration.skill.SkillMarkdownParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SkillServiceImpl extends ServiceImpl<SkillMapper, SfSkill> implements SkillService {

    private final SkillMapper skillMapper;
    private final ToolExecutionService toolExecutionService;

    @Override
    public boolean save(SfSkill entity) {
        validateSkillEntity(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(SfSkill entity) {
        validateSkillEntity(entity);
        return super.updateById(entity);
    }

    private void validateSkillEntity(SfSkill entity) {
        if (entity == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill entity is required");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill name is required");
        }
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill code is required");
        }
    }

    @Override
    public SfSkill createVersion(Long skillId, String content, String changeNote) {
        SfSkill existing = getById(skillId);
        if (existing == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Skill not found: " + skillId);
        }
        if (content == null || content.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill content is required");
        }

        // Find latest version for this skill code
        LambdaQueryWrapper<SfSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSkill::getCode, existing.getCode())
               .orderByDesc(SfSkill::getId)
               .last("LIMIT 1");
        SfSkill latest = skillMapper.selectOne(wrapper);
        long nextVersion = (latest != null && latest.getId() != null) ? latest.getId() + 1 : 1;

        SfSkill newVersion = new SfSkill();
        newVersion.setName(existing.getName());
        newVersion.setCode(existing.getCode() + "@v" + nextVersion);
        newVersion.setDescription(changeNote != null ? changeNote : "Version " + nextVersion);
        newVersion.setContent(content);
        newVersion.setStatus(1);
        skillMapper.insert(newVersion);

        log.info("Created skill version {} for code {}", nextVersion, existing.getCode());
        return newVersion;
    }

    @Override
    public List<SfSkill> listVersions(String skillCode) {
        if (skillCode == null || skillCode.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill code is required");
        }
        LambdaQueryWrapper<SfSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SfSkill::getCode, skillCode)
               .orderByAsc(SfSkill::getId);
        return skillMapper.selectList(wrapper);
    }

    @Override
    public boolean validateSkill(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        try {
            SkillMarkdownParser.parseMeta(content);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Skill validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String executeSkill(Long skillId, Map<String, Object> parameters) {
        SfSkill skill = getById(skillId);
        if (skill == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Skill not found: " + skillId);
        }
        if (skill.getContent() == null || skill.getContent().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill content is empty");
        }

        // Validate syntax before execution
        if (!validateSkill(skill.getContent())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Skill content failed validation");
        }

        try {
            // Execute skill as a "local" tool with the skill content as parameter
            Map<String, Object> toolParams = Map.of(
                    "action", "skill",
                    "skillId", skillId,
                    "skillName", skill.getName(),
                    "content", skill.getContent(),
                    "parameters", parameters != null ? parameters : Map.of()
            );
            String paramsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toolParams);
            return toolExecutionService.executeTool("local", paramsJson);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Skill execution failed: skillId={}", skillId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Skill execution failed: " + e.getMessage());
        }
    }

    @Override
    public List<SkillSummary> listSummaries() {
        List<SfSkill> skills = list();
        return skills.stream()
                .map(SkillSummary::from)
                .toList();
    }

    @Override
    public SkillSummary getSummaryById(Long id) {
        SfSkill skill = getById(id);
        return SkillSummary.from(skill);
    }

    @Override
    public SkillContent getContent(Long id) {
        SfSkill skill = getById(id);
        return SkillContent.from(skill);
    }
}
