package com.schemaplexai.spec.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecVersionMapper;
import com.schemaplexai.spec.service.impl.SpecVersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecVersionServiceImplTest {

    @Mock
    private SfSpecMapper specMapper;

    @Mock
    private SfSpecVersionMapper specVersionMapper;

    @InjectMocks
    private SpecVersionServiceImpl specVersionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(specVersionService, "baseMapper", specVersionMapper);
    }

    // ------------------------------------------------------------------
    // diff
    // ------------------------------------------------------------------

    @Test
    void diff_versionANull_throwsSpecNotFound() {
        when(specVersionMapper.selectById(1L)).thenReturn(null);
        when(specVersionMapper.selectById(2L)).thenReturn(new SfSpecVersion());

        assertThatThrownBy(() -> specVersionService.diff(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void diff_versionBNull_throwsSpecNotFound() {
        when(specVersionMapper.selectById(1L)).thenReturn(new SfSpecVersion());
        when(specVersionMapper.selectById(2L)).thenReturn(null);

        assertThatThrownBy(() -> specVersionService.diff(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void diff_differentSpecs_throwsParamError() {
        SfSpecVersion vA = new SfSpecVersion();
        vA.setSpecId(1L);
        vA.setContent("old");
        SfSpecVersion vB = new SfSpecVersion();
        vB.setSpecId(2L);
        vB.setContent("new");
        when(specVersionMapper.selectById(1L)).thenReturn(vA);
        when(specVersionMapper.selectById(2L)).thenReturn(vB);

        assertThatThrownBy(() -> specVersionService.diff(1L, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void diff_sameSpecs_returnsDiffResult() {
        SfSpecVersion vA = new SfSpecVersion();
        vA.setSpecId(1L);
        vA.setContent("line1\nline2");
        SfSpecVersion vB = new SfSpecVersion();
        vB.setSpecId(1L);
        vB.setContent("line1\nline3");
        when(specVersionMapper.selectById(1L)).thenReturn(vA);
        when(specVersionMapper.selectById(2L)).thenReturn(vB);

        SpecDiffResult result = specVersionService.diff(1L, 2L);

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getVersionAId()).isEqualTo(1L);
        assertThat(result.getVersionBId()).isEqualTo(2L);
        assertThat(result.getHunks()).isNotNull();
    }

    // ------------------------------------------------------------------
    // createVersion
    // ------------------------------------------------------------------

    @Test
    void createVersion_specNotFound_throwsSpecNotFound() {
        when(specMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> specVersionService.createVersion(1L, "v1", "content", "initial"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.SPEC_NOT_FOUND.getCode());
    }

    @Test
    void createVersion_success_createsVersionAndUpdatesSpec() {
        SfSpec spec = new SfSpec();
        spec.setId(1L);
        spec.setStatus("DRAFT");
        when(specMapper.selectById(1L)).thenReturn(spec);
        when(specVersionMapper.insert(any())).thenReturn(1);

        SfSpecVersion result = specVersionService.createVersion(1L, "v1", "new content", "first version");

        assertThat(result.getSpecId()).isEqualTo(1L);
        assertThat(result.getVersion()).isEqualTo("v1");
        assertThat(result.getContent()).isEqualTo("new content");
        assertThat(result.getChangeLog()).isEqualTo("first version");
        assertThat(spec.getContent()).isEqualTo("new content");
        assertThat(spec.getStatus()).isEqualTo("ACTIVE");
        verify(specMapper).updateById(spec);
        verify(specVersionMapper).insert(any());
    }
}
