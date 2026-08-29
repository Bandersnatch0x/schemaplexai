package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.MilvusSyncMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpMilvusSyncRequestHandlerTest {

    @Mock
    private ContextServiceClient contextServiceClient;

    @InjectMocks
    private HttpMilvusSyncRequestHandler handler;

    @Test
    void handle_validMessage_delegatesToContextService() {
        MilvusSyncMessage message = new MilvusSyncMessage();
        message.setOperation("SYNC_DOC");
        message.setDocId(11L);
        message.setTenantId("tenant-a");

        handler.handle(message);

        verify(contextServiceClient).syncDocument(11L, "tenant-a");
    }

    @Test
    void handle_nullMessage_throwsParamError() {
        assertThatThrownBy(() -> handler.handle(null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verifyNoInteractions(contextServiceClient);
    }

    @Test
    void handle_missingDocId_throwsParamError() {
        MilvusSyncMessage message = new MilvusSyncMessage();
        message.setOperation("SYNC_DOC");
        message.setTenantId("tenant-a");

        assertThatThrownBy(() -> handler.handle(message))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).contains("docId"));

        verifyNoInteractions(contextServiceClient);
    }

    @Test
    void handle_blankTenantId_throwsParamErrorFailClosed() {
        MilvusSyncMessage message = new MilvusSyncMessage();
        message.setOperation("SYNC_DOC");
        message.setDocId(11L);
        message.setTenantId("  ");

        assertThatThrownBy(() -> handler.handle(message))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).contains("tenantId"));

        verifyNoInteractions(contextServiceClient);
    }

    @Test
    void handle_clientFailure_propagatesForNackAndDlq() {
        MilvusSyncMessage message = new MilvusSyncMessage();
        message.setOperation("SYNC_DOC");
        message.setDocId(11L);
        message.setTenantId("tenant-a");
        doThrow(new BaseException(ResultCode.INTERNAL_ERROR, "context service down"))
                .when(contextServiceClient).syncDocument(11L, "tenant-a");

        assertThatThrownBy(() -> handler.handle(message))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("context service down");
    }
}
