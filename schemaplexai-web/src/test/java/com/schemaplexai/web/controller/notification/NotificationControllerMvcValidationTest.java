package com.schemaplexai.web.controller.notification;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.exception.GlobalExceptionHandler;
import com.schemaplexai.web.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue 926 / REQ-12: current/size of the notification page endpoint are
 * constraint-validated (current >= 1, 1 <= size <= 100). Out-of-range values
 * must be rejected with the param-error envelope (code 400) before any
 * service call happens. Review ST-02: the page number param is named
 * {@code current} and size defaults to 10, per the repository API spec.
 */
@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {NotificationController.class, GlobalExceptionHandler.class})
@DisplayName("926: notification pagination parameter validation (MockMvc)")
class NotificationControllerMvcValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("valid pagination params pass through with code 200")
    void page_validParams_returnsSuccess() throws Exception {
        when(notificationService.pageQuery(100L, 1, 20, null)).thenReturn(new Page<>());

        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("current", "1")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("omitted params default to current=1, size=10 per API spec (review ST-02)")
    void page_defaultParams_currentOneSizeTen() throws Exception {
        when(notificationService.pageQuery(100L, 1, 10, null)).thenReturn(new Page<>());

        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(notificationService).pageQuery(100L, 1, 10, null);
    }

    @Test
    @DisplayName("size above the 100 cap is rejected with code 400")
    void page_sizeAboveMax_returns400() throws Exception {
        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("current", "1")
                        .param("size", "101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("size=0 is rejected with code 400")
    void page_sizeZero_returns400() throws Exception {
        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("negative size is rejected with code 400")
    void page_negativeSize_returns400() throws Exception {
        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("size", "-5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("current=0 is rejected with code 400")
    void page_currentZero_returns400() throws Exception {
        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("current", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("size=100 is accepted as the upper boundary")
    void page_sizeAtUpperBound_isAccepted() throws Exception {
        when(notificationService.pageQuery(100L, 1, 100, null)).thenReturn(new Page<>());

        mockMvc.perform(get("/web/notification/page")
                        .header("X-User-Id", "100")
                        .param("size", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
