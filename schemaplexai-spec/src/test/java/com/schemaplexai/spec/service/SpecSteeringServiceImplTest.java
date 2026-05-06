package com.schemaplexai.spec.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpecSteering;
import com.schemaplexai.spec.mapper.SfSpecSteeringMapper;
import com.schemaplexai.spec.service.impl.SpecSteeringServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecSteeringServiceImplTest {

    @Mock
    private SfSpecSteeringMapper specSteeringMapper;

    @InjectMocks
    private SpecSteeringServiceImpl specSteeringService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specSteeringService, "baseMapper", specSteeringMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // evaluateSteeringRules
    // ------------------------------------------------------------------

    @Test
    void evaluateSteeringRules_nullContent_throwsParamError() {
        assertThatThrownBy(() -> specSteeringService.evaluateSteeringRules(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void evaluateSteeringRules_blankContent_throwsParamError() {
        assertThatThrownBy(() -> specSteeringService.evaluateSteeringRules(1L, "   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void evaluateSteeringRules_emptyContent_throwsParamError() {
        assertThatThrownBy(() -> specSteeringService.evaluateSteeringRules(1L, ""))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void evaluateSteeringRules_noSteerings_returnsEmptyMap() {
        when(specSteeringMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Boolean> result = specSteeringService.evaluateSteeringRules(1L, "some content");

        assertThat(result).isEmpty();
    }

    @Test
    void evaluateSteeringRules_withMatches_returnsTrueForMatches() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setId(10L);
        steering.setDirection("go left");
        steering.setConstraints("max 100");
        steering.setAcceptanceCriteria("done");
        when(specSteeringMapper.selectList(any())).thenReturn(List.of(steering));

        Map<String, Boolean> result = specSteeringService.evaluateSteeringRules(1L, "go left and max 100");

        assertThat(result).containsEntry("direction_10", true);
        assertThat(result).containsEntry("constraints_10", true);
        assertThat(result).containsEntry("criteria_10", false);
    }

    @Test
    void evaluateSteeringRules_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(specSteeringMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Boolean> result = specSteeringService.evaluateSteeringRules(1L, "content");

        assertThat(result).isEmpty();
    }

    @Test
    void evaluateSteeringRules_nullSteeringFields_handlesGracefully() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setId(10L);
        steering.setDirection(null);
        steering.setConstraints(null);
        steering.setAcceptanceCriteria(null);
        when(specSteeringMapper.selectList(any())).thenReturn(List.of(steering));

        Map<String, Boolean> result = specSteeringService.evaluateSteeringRules(1L, "any content");

        assertThat(result).containsEntry("direction_10", false);
        assertThat(result).containsEntry("constraints_10", false);
        assertThat(result).containsEntry("criteria_10", false);
    }

    // ------------------------------------------------------------------
    // applySteering
    // ------------------------------------------------------------------

    @Test
    void applySteering_nullContent_throwsParamError() {
        assertThatThrownBy(() -> specSteeringService.applySteering(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void applySteering_noSteerings_returnsOriginalContent() {
        when(specSteeringMapper.selectList(any())).thenReturn(Collections.emptyList());

        String result = specSteeringService.applySteering(1L, "original content");

        assertThat(result).isEqualTo("original content");
    }

    @Test
    void applySteering_withSteerings_appendsMissingRules() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setId(10L);
        steering.setDirection("go left");
        steering.setConstraints("max 100");
        steering.setAcceptanceCriteria("done");
        when(specSteeringMapper.selectList(any())).thenReturn(List.of(steering));

        String result = specSteeringService.applySteering(1L, "original");

        assertThat(result).contains("original");
        assertThat(result).contains("<!-- Direction: go left -->");
        assertThat(result).contains("<!-- Constraints: max 100 -->");
        assertThat(result).contains("<!-- Acceptance Criteria: done -->");
    }

    @Test
    void applySteering_alreadyContainsDirection_doesNotDuplicate() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setId(10L);
        steering.setDirection("go left");
        steering.setConstraints("max 100");
        when(specSteeringMapper.selectList(any())).thenReturn(List.of(steering));

        String result = specSteeringService.applySteering(1L, "already has go left in it");

        assertThat(result).contains("<!-- Constraints: max 100 -->");
        assertThat(result).doesNotContain("<!-- Direction: go left -->");
    }

    // ------------------------------------------------------------------
    // listActiveSteerings
    // ------------------------------------------------------------------

    @Test
    void listActiveSteerings_returnsSteeringsWithAnyFieldSet() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setDirection("dir");
        when(specSteeringMapper.selectList(any())).thenReturn(List.of(steering));

        List<SfSpecSteering> result = specSteeringService.listActiveSteerings(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void listActiveSteerings_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(specSteeringMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfSpecSteering> result = specSteeringService.listActiveSteerings(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // validateSteeringConfig
    // ------------------------------------------------------------------

    @Test
    void validateSteeringConfig_notFound_throwsNotFound() {
        when(specSteeringMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specSteeringService.validateSteeringConfig(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void validateSteeringConfig_allFieldsSet_returnsTrue() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setDirection("dir");
        steering.setConstraints("con");
        steering.setAcceptanceCriteria("crit");
        when(specSteeringMapper.selectById(1L)).thenReturn(steering);

        boolean result = specSteeringService.validateSteeringConfig(1L);

        assertThat(result).isTrue();
    }

    @Test
    void validateSteeringConfig_onlyOneFieldSet_returnsTrue() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setDirection("dir");
        when(specSteeringMapper.selectById(1L)).thenReturn(steering);

        boolean result = specSteeringService.validateSteeringConfig(1L);

        assertThat(result).isTrue();
    }

    @Test
    void validateSteeringConfig_noFieldsSet_returnsFalse() {
        SfSpecSteering steering = new SfSpecSteering();
        when(specSteeringMapper.selectById(1L)).thenReturn(steering);

        boolean result = specSteeringService.validateSteeringConfig(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateSteeringConfig_blankFields_returnsFalse() {
        SfSpecSteering steering = new SfSpecSteering();
        steering.setDirection("   ");
        steering.setConstraints("");
        when(specSteeringMapper.selectById(1L)).thenReturn(steering);

        boolean result = specSteeringService.validateSteeringConfig(1L);

        assertThat(result).isFalse();
    }
}
