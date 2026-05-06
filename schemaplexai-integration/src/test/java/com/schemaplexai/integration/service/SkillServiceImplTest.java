package com.schemaplexai.integration.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfSkill;
import com.schemaplexai.integration.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillMapper skillMapper;

    @Mock
    private ToolExecutionService toolExecutionService;

    @InjectMocks
    private SkillServiceImpl skillService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(skillService, "baseMapper", skillMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullName_throwsParamError() {
        SfSkill skill = new SfSkill();
        skill.setCode("my-skill");

        assertThatThrownBy(() -> skillService.save(skill))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullCode_throwsParamError() {
        SfSkill skill = new SfSkill();
        skill.setName("My Skill");

        assertThatThrownBy(() -> skillService.save(skill))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success() {
        SfSkill skill = new SfSkill();
        skill.setName("My Skill");
        skill.setCode("my-skill");
        when(skillMapper.insert(any())).thenReturn(1);

        boolean result = skillService.save(skill);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // createVersion
    // ------------------------------------------------------------------

    @Test
    void createVersion_notFound_throwsNotFound() {
        when(skillMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> skillService.createVersion(1L, "content", "note"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createVersion_nullContent_throwsParamError() {
        SfSkill existing = new SfSkill();
        existing.setId(1L);
        existing.setCode("my-skill");
        when(skillMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> skillService.createVersion(1L, null, "note"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createVersion_success_createsNewVersion() {
        SfSkill existing = new SfSkill();
        existing.setId(1L);
        existing.setName("My Skill");
        existing.setCode("my-skill");
        when(skillMapper.selectById(1L)).thenReturn(existing);
        SfSkill latest = new SfSkill();
        latest.setId(3L);
        when(skillMapper.selectOne(any())).thenReturn(latest);
        when(skillMapper.insert(any())).thenReturn(1);

        SfSkill result = skillService.createVersion(1L, "new content", "v4 note");

        assertThat(result.getCode()).isEqualTo("my-skill@v4");
        assertThat(result.getContent()).isEqualTo("new content");
        verify(skillMapper).insert(any());
    }

    // ------------------------------------------------------------------
    // listVersions
    // ------------------------------------------------------------------

    @Test
    void listVersions_nullCode_throwsParamError() {
        assertThatThrownBy(() -> skillService.listVersions(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listVersions_success_returnsVersions() {
        SfSkill skill = new SfSkill();
        skill.setCode("my-skill");
        when(skillMapper.selectList(any())).thenReturn(List.of(skill));

        List<SfSkill> result = skillService.listVersions("my-skill");

        assertThat(result).hasSize(1);
    }

    @Test
    void listVersions_empty_returnsEmpty() {
        when(skillMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfSkill> result = skillService.listVersions("my-skill");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // validateSkill
    // ------------------------------------------------------------------

    @Test
    void validateSkill_nullContent_returnsFalse() {
        assertThat(skillService.validateSkill(null)).isFalse();
    }

    @Test
    void validateSkill_blankContent_returnsFalse() {
        assertThat(skillService.validateSkill("   ")).isFalse();
    }

    // ------------------------------------------------------------------
    // executeSkill
    // ------------------------------------------------------------------

    @Test
    void executeSkill_notFound_throwsNotFound() {
        when(skillMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> skillService.executeSkill(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void executeSkill_nullContent_throwsParamError() {
        SfSkill skill = new SfSkill();
        skill.setId(1L);
        skill.setContent(null);
        when(skillMapper.selectById(1L)).thenReturn(skill);

        assertThatThrownBy(() -> skillService.executeSkill(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void executeSkill_invalidContent_throwsParamError() {
        SfSkill skill = new SfSkill();
        skill.setId(1L);
        skill.setContent("invalid content without metadata");
        when(skillMapper.selectById(1L)).thenReturn(skill);

        assertThatThrownBy(() -> skillService.executeSkill(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }
}
