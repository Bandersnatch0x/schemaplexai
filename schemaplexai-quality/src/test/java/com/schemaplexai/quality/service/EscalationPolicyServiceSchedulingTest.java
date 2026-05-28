package com.schemaplexai.quality.service;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EscalationPolicyServiceSchedulingTest {

    @Test
    void checkEscalationsDelaysFirstRunUntilStartupSettles() throws NoSuchMethodException {
        Method method = EscalationPolicyService.class.getDeclaredMethod("checkEscalations");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("${approval.escalation.check.initial-delay-ms:300000}", scheduled.initialDelayString());
    }
}
