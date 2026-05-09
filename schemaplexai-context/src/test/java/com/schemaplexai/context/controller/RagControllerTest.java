package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.service.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @InjectMocks
    private RagController controller;

    @Test
    void retrieve_success() {
        when(ragService.retrieve("test query", 5)).thenReturn(List.of("chunk1", "chunk2"));
        RagController.RetrieveRequest request = new RagController.RetrieveRequest();
        request.setQuery("test query");
        request.setTopK(5);
        Result<List<String>> result = controller.retrieve(request);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly("chunk1", "chunk2");
    }
}
