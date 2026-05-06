package com.schemaplexai.integration.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.impl.McpServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpServerServiceImplTest {

    @Mock
    private McpServerMapper mcpServerMapper;

    @InjectMocks
    private McpServerServiceImpl mcpServerService;

    @BeforeEach
    void setUp() {
        // McpServerServiceImpl extends ServiceImpl which stores mapper in baseMapper field.
        // @InjectMocks can't inject into parent class fields, so set it manually.
        ReflectionTestUtils.setField(mcpServerService, "baseMapper", mcpServerMapper);
    }

    @Test
    void validateEndpoint_nullEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_blankEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_emptyEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint(""))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_httpEndpoint_passes() {
        mcpServerService.validateEndpoint("http://localhost:8080/mcp");
    }

    @Test
    void validateEndpoint_httpsEndpoint_passes() {
        mcpServerService.validateEndpoint("https://mcp.example.com/api");
    }

    @Test
    void validateEndpoint_ftpEndpoint_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("ftp://server/file"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void validateEndpoint_noProtocol_throwsParamError() {
        assertThatThrownBy(() -> mcpServerService.validateEndpoint("localhost:8080/mcp"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void healthCheck_serverNotFound_returnsFalse() {
        when(mcpServerMapper.selectById(100L)).thenReturn(null);

        boolean result = mcpServerService.healthCheck(100L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_nullEndpoint_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(200L);
        server.setEndpoint(null);
        when(mcpServerMapper.selectById(200L)).thenReturn(server);

        boolean result = mcpServerService.healthCheck(200L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_blankEndpoint_returnsFalse() {
        SfMcpServer server = new SfMcpServer();
        server.setId(300L);
        server.setEndpoint("");
        when(mcpServerMapper.selectById(300L)).thenReturn(server);

        boolean result = mcpServerService.healthCheck(300L);

        assertThat(result).isFalse();
    }
}
