package com.schemaplexai.workflow.service;

import com.schemaplexai.workflow.service.impl.WorkflowInstanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Failure-state persistence must survive exceptions. If the orchestration or node
 * execution ran inside a transaction, the FAILED status written on the exception path
 * would be rolled back together with the rethrown exception, leaving no trace of the
 * failure in the database. These tests pin the invariant that neither entry point is
 * annotated @Transactional, so every status write commits independently.
 */
class FailurePersistenceSemanticsTest {

    @Test
    void trigger_isNotTransactional() throws NoSuchMethodException {
        Method trigger = WorkflowInstanceServiceImpl.class.getMethod("trigger", Long.class);
        assertThat(trigger.isAnnotationPresent(Transactional.class))
                .as("trigger() must not run in a transaction so FAILED status persists on exception paths")
                .isFalse();
    }

    @Test
    void executeNode_isNotTransactional() throws NoSuchMethodException {
        Method executeNode = WorkflowNodeEngine.class.getMethod(
                "executeNode", com.schemaplexai.workflow.entity.SfWorkflowNodeExecution.class);
        assertThat(executeNode.isAnnotationPresent(Transactional.class))
                .as("executeNode() must not run in a transaction so FAILED status persists on exception paths")
                .isFalse();
    }
}
