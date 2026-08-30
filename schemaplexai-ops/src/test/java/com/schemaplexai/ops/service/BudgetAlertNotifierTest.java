package com.schemaplexai.ops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAlertNotifierTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BudgetAlertNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new BudgetAlertNotifier(notificationMapper, rabbitTemplate, new ObjectMapper());
    }

    @Test
    void dispatch_persistsAlertRecordWithExpectedFields() {
        SfBudget budget = budget("tenant-1", "MONTHLY", new BigDecimal("100"), new BigDecimal("150"));

        boolean dispatched = notifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_EXCEEDED,
                new BigDecimal("150.000000"));

        assertTrue(dispatched);
        ArgumentCaptor<SfNotification> captor = ArgumentCaptor.forClass(SfNotification.class);
        verify(notificationMapper).insert(captor.capture());
        SfNotification record = captor.getValue();
        assertEquals(BudgetAlertNotifier.ALERT_NOTIFICATION_TYPE, record.getType());
        assertEquals("tenant-1", record.getTenantId());
        assertEquals(BudgetAlertNotifier.BROADCAST_USER_ID, record.getUserId());
        assertFalse(record.getRead());
        assertTrue(record.getTitle().contains(BudgetAlertNotifier.LEVEL_EXCEEDED));
        assertTrue(record.getContent().contains("150"));
    }

    @Test
    void dispatch_publishesNotificationEventOnUnifiedRoute() {
        SfBudget budget = budget("tenant-1", "MONTHLY", new BigDecimal("100"), new BigDecimal("90"));

        notifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_THRESHOLD_REACHED,
                new BigDecimal("90.000000"));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_NOTIFICATION),
                payload.capture());
        assertTrue(payload.getValue().contains("\"channel\":\"in-app\""));
        assertTrue(payload.getValue().contains(BudgetAlertNotifier.LEVEL_THRESHOLD_REACHED));
    }

    @Test
    void dispatch_duplicateWithinWindow_suppressesPersistAndPublish() {
        SfBudget budget = budget("tenant-1", "MONTHLY", new BigDecimal("100"), new BigDecimal("150"));
        when(notificationMapper.selectCount(any())).thenReturn(1L);

        boolean dispatched = notifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_EXCEEDED,
                new BigDecimal("150.000000"));

        assertFalse(dispatched, "Same budget+level already alerted within the dedup window");
        verify(notificationMapper, never()).insert(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void dispatch_mqFailure_stillPersistsAndSucceeds() {
        SfBudget budget = budget("tenant-1", "MONTHLY", new BigDecimal("100"), new BigDecimal("150"));
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        boolean dispatched = notifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_EXCEEDED,
                new BigDecimal("150.000000"));

        assertTrue(dispatched, "Dual-track: persistence + log must survive a broker outage");
        verify(notificationMapper).insert(any());
    }

    @Test
    void dispatch_budgetWithoutId_usesTypeIdentifier() {
        SfBudget budget = budget("tenant-1", "AGENT", new BigDecimal("100"), new BigDecimal("150"));
        budget.setId(null);

        notifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_EXCEEDED, new BigDecimal("150"));

        ArgumentCaptor<SfNotification> captor = ArgumentCaptor.forClass(SfNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertTrue(captor.getValue().getTitle().contains("budgetType=AGENT"));
    }

    private SfBudget budget(String tenantId, String type, BigDecimal limit, BigDecimal used) {
        SfBudget budget = new SfBudget();
        budget.setId(42L);
        budget.setTenantId(tenantId);
        budget.setBudgetType(type);
        budget.setLimitAmount(limit);
        budget.setUsedAmount(used);
        budget.setAlertThreshold(new BigDecimal("0.8"));
        return budget;
    }
}
