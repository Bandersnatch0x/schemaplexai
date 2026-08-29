package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.quality.gate.GateDisposition;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.quality.service.QualityOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ticket 924 / REQ-01: the quality module consumes engine check requests from
 * sf.quality, evaluates in-process, and publishes the structured verdict.
 */
@ExtendWith(MockitoExtension.class)
class QualityCheckEventConsumerTest {

    @Mock
    private QualityOrchestrator qualityOrchestrator;

    @Mock
    private QualityVerdictPublisher verdictPublisher;

    @Mock
    private InboxDeduplicationService inboxDeduplicationService;

    private QualityCheckEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new QualityCheckEventConsumer(qualityOrchestrator, verdictPublisher,
                inboxDeduplicationService, objectMapper);
    }

    private Message message(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8));
    }

    private String checkRequest(String eventId, Long executionId) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("eventType", "QUALITY_CHECK_REQUEST");
        if (eventId != null) {
            node.put("eventId", eventId);
        }
        node.put("executionId", executionId);
        node.put("agentId", 42L);
        node.put("tenantId", "3");
        node.put("triggerPoint", "POST_EXECUTION");
        node.put("output", "Final Answer: ok");
        node.put("securityScanCompleted", true);
        node.put("securityScanPassed", true);
        return objectMapper.writeValueAsString(node);
    }

    @Test
    void validRequest_evaluatesAndPublishesVerdict() throws Exception {
        String eventId = UUID.randomUUID().toString();
        QualityReport report = new QualityReport(7L, true, List.of(QualityCheckResult.pass()),
                GateDisposition.PASS);
        when(qualityOrchestrator.evaluate(eq(7L), any(QualityContext.class))).thenReturn(report);

        consumer.onMessage(message(checkRequest(eventId, 7L)), mock(Channel.class));

        ArgumentCaptor<QualityContext> context = ArgumentCaptor.forClass(QualityContext.class);
        verify(qualityOrchestrator).evaluate(eq(7L), context.capture());
        assertThat(context.getValue().getMetadata().get("output")).isEqualTo("Final Answer: ok");
        assertThat(context.getValue().getMetadata().get("securityScanCompleted")).isEqualTo(true);
        assertThat(context.getValue().getMetadata().get("triggerPoint")).isEqualTo("POST_EXECUTION");

        verify(verdictPublisher).publishVerdict(eq(7L), eq(42L), eq("3"), eq("POST_EXECUTION"), eq(report));
        verify(inboxDeduplicationService).markProcessed(UUID.fromString(eventId), "QualityCheckEventConsumer");
        // tenant context must not leak past the message
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void duplicateEventId_isSkippedIdempotently() throws Exception {
        String eventId = UUID.randomUUID().toString();
        when(inboxDeduplicationService.isProcessed(UUID.fromString(eventId), "QualityCheckEventConsumer"))
                .thenReturn(true);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message(checkRequest(eventId, 8L)), channel);

        verify(qualityOrchestrator, never()).evaluate(anyLong(), any());
        verify(verdictPublisher, never()).publishVerdict(anyLong(), any(), any(), any(), any());
        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    void missingExecutionId_isDroppedWithoutEvaluation() throws Exception {
        Channel channel = mock(Channel.class);
        consumer.onMessage(message("{\"eventType\":\"QUALITY_CHECK_REQUEST\",\"tenantId\":\"3\"}"), channel);

        verify(qualityOrchestrator, never()).evaluate(anyLong(), any());
        verify(channel).basicAck(anyLong(), eq(false));
    }

    @Test
    void malformedPayload_isNackedWithoutRequeue() throws Exception {
        Channel channel = mock(Channel.class);
        consumer.onMessage(message("not-json"), channel);

        verify(qualityOrchestrator, never()).evaluate(anyLong(), any());
        verify(channel).basicNack(anyLong(), eq(false), eq(false));
    }

    @Test
    void evaluationFailure_isNackedWithoutRequeue() throws Exception {
        String eventId = UUID.randomUUID().toString();
        when(qualityOrchestrator.evaluate(anyLong(), any(QualityContext.class)))
                .thenThrow(new RuntimeException("db down"));

        Channel channel = mock(Channel.class);
        consumer.onMessage(message(checkRequest(eventId, 9L)), channel);

        verify(channel).basicNack(anyLong(), eq(false), eq(false));
        verify(verdictPublisher, never()).publishVerdict(anyLong(), any(), anyString(), anyString(), any());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void requestWithoutEventId_isStillProcessed() throws Exception {
        QualityReport report = new QualityReport(10L, true, List.of(), GateDisposition.PASS);
        when(qualityOrchestrator.evaluate(eq(10L), any(QualityContext.class))).thenReturn(report);

        Channel channel = mock(Channel.class);
        consumer.onMessage(message(checkRequest(null, 10L)), channel);

        verify(qualityOrchestrator).evaluate(eq(10L), any(QualityContext.class));
        verify(verdictPublisher).publishVerdict(eq(10L), eq(42L), eq("3"), eq("POST_EXECUTION"), eq(report));
        verify(inboxDeduplicationService, never()).markProcessed(any(), anyString());
        verify(channel).basicAck(anyLong(), eq(false));
    }
}
