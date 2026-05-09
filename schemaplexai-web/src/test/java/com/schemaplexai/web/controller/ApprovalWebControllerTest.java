package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.4: Approval Web Controller Tests")
class ApprovalWebControllerTest {

    @InjectMocks
    private ApprovalWebController controller;

    @Test
    @DisplayName("GET /web/approvals returns list of pending approvals")
    void listPendingApprovals_returnsSuccessResult() {
        Result<List<Map<String, Object>>> result = controller.listPendingApprovals();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).get("ticketId")).isEqualTo("TICKET-001");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/approve returns success")
    void approve_returnsSuccessResult() {
        Result<Void> result = controller.approve("TICKET-001", "Approved by manager");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/reject returns success")
    void reject_returnsSuccessResult() {
        Result<Void> result = controller.reject("TICKET-001", "Insufficient budget");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/escalate returns success")
    void escalate_returnsSuccessResult() {
        Result<Void> result = controller.escalate("TICKET-001");

        assertThat(result.getCode()).isEqualTo(200);
    }
}
