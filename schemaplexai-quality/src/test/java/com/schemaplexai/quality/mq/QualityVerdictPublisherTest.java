package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.quality.gate.GateDisposition;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Ticket 924 / REQ-02: the verdict published back to the engine carries the
 * structured disposition the engine acts on (PASS/WARN/BLOCK/FAIL).
 */
@ExtendWith(MockitoExtension.class)
class QualityVerdictPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private QualityVerdictPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new QualityVerdictPublisher(rabbitTemplate, objectMapper);
    }

    @Test
    void publishVerdictSendsDispositionOnVerdictRoutingKey() throws Exception {
        QualityCheckResult failed = QualityCheckResult.fail("HIGH", "Missing security scan evidence",
                GateDisposition.BLOCK);
        failed.setRuleName("SECURITY_SCAN");
        QualityReport report = new QualityReport(7L, false, List.of(failed), GateDisposition.BLOCK);

        boolean sent = publisher.publishVerdict(7L, 42L, "3", "POST_EXECUTION", report);

        assertTrue(sent);
        ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(), body.capture());

        assertThat(exchange.getValue()).isEqualTo(CommonConstants.EXCHANGE_SCHEMAPLEXAI);
        assertThat(routingKey.getValue()).isEqualTo(QualityVerdictPublisher.RK_QUALITY_VERDICT);

        JsonNode payload = objectMapper.readTree(body.getValue());
        assertThat(payload.get("eventType").asText()).isEqualTo(QualityVerdictPublisher.EVENT_TYPE_VERDICT);
        assertThat(payload.get("executionId").asLong()).isEqualTo(7L);
        assertThat(payload.get("agentId").asLong()).isEqualTo(42L);
        assertThat(payload.get("tenantId").asText()).isEqualTo("3");
        assertThat(payload.get("triggerPoint").asText()).isEqualTo("POST_EXECUTION");
        assertThat(payload.get("disposition").asText()).isEqualTo("BLOCK");
        assertThat(payload.get("allPassed").asBoolean()).isFalse();
        assertThat(payload.get("issueCount").asInt()).isEqualTo(1);
        assertThat(payload.get("failedRules")).hasSize(1);
        assertThat(payload.get("failedRules").get(0).asText()).isEqualTo("SECURITY_SCAN");
        assertThat(payload.hasNonNull("eventId")).isTrue();
        assertThat(payload.hasNonNull("checkedAt")).isTrue();
    }

    @Test
    void publishVerdictForPassingReportSendsPassDisposition() throws Exception {
        QualityReport report = new QualityReport(8L, true, List.of(QualityCheckResult.pass()),
                GateDisposition.PASS);

        publisher.publishVerdict(8L, 1L, "1", "POST_EXECUTION", report);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(QualityVerdictPublisher.RK_QUALITY_VERDICT), body.capture());
        JsonNode payload = objectMapper.readTree(body.getValue());
        assertThat(payload.get("disposition").asText()).isEqualTo("PASS");
        assertThat(payload.get("allPassed").asBoolean()).isTrue();
        assertThat(payload.get("issueCount").asInt()).isZero();
    }

    @Test
    void mqFailureNeverThrowsAndReturnsFalse() {
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
        QualityReport report = new QualityReport(9L, true, List.of(), GateDisposition.PASS);

        assertDoesNotThrow(() -> publisher.publishVerdict(9L, 1L, "1", "POST_EXECUTION", report));
        assertFalse(publisher.publishVerdict(9L, 1L, "1", "POST_EXECUTION", report));
    }
}
