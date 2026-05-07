package com.schemaplexai.agent.engine.plan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubTaskPlanTest {

    private SubTaskPlan plan;

    @BeforeEach
    void setUp() {
        plan = new SubTaskPlan();
        plan.setGoal("Build a web app");
    }

    @Test
    void findNextReadySubTaskShouldReturnFirstPendingWithNoDeps() {
        SubTask st1 = createSubTask("st-1", "Design UI", SubTask.STATUS_PENDING, List.of());
        SubTask st2 = createSubTask("st-2", "Implement backend", SubTask.STATUS_PENDING, List.of("st-1"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        SubTask next = plan.findNextReadySubTask();

        assertNotNull(next);
        assertEquals("st-1", next.getId());
    }

    @Test
    void findNextReadySubTaskShouldReturnTaskWithSatisfiedDeps() {
        SubTask st1 = createSubTask("st-1", "Design UI", SubTask.STATUS_COMPLETED, List.of());
        SubTask st2 = createSubTask("st-2", "Implement backend", SubTask.STATUS_PENDING, List.of("st-1"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        SubTask next = plan.findNextReadySubTask();

        assertNotNull(next);
        assertEquals("st-2", next.getId());
    }

    @Test
    void findNextReadySubTaskShouldReturnNullWhenDepsNotSatisfied() {
        SubTask st1 = createSubTask("st-1", "Design UI", SubTask.STATUS_PENDING, List.of());
        SubTask st2 = createSubTask("st-2", "Implement backend", SubTask.STATUS_PENDING, List.of("st-1"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        // st-1 is still PENDING, so st-2 is blocked
        SubTask next = plan.findNextReadySubTask();

        assertNotNull(next);
        assertEquals("st-1", next.getId());
    }

    @Test
    void findNextReadySubTaskShouldReturnNullWhenAllCompleted() {
        SubTask st1 = createSubTask("st-1", "Design UI", SubTask.STATUS_COMPLETED, List.of());
        SubTask st2 = createSubTask("st-2", "Implement backend", SubTask.STATUS_COMPLETED, List.of("st-1"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        SubTask next = plan.findNextReadySubTask();

        assertNull(next);
    }

    @Test
    void findNextReadySubTaskShouldSkipInProgressTasks() {
        SubTask st1 = createSubTask("st-1", "Design UI", SubTask.STATUS_IN_PROGRESS, List.of());
        SubTask st2 = createSubTask("st-2", "Implement backend", SubTask.STATUS_PENDING, List.of("st-1"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        SubTask next = plan.findNextReadySubTask();

        assertNull(next);
    }

    @Test
    void findNextReadySubTaskShouldHandleMultiplePendingWithMixedDeps() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_COMPLETED, List.of());
        SubTask st2 = createSubTask("st-2", "Task B", SubTask.STATUS_COMPLETED, List.of());
        SubTask st3 = createSubTask("st-3", "Task C", SubTask.STATUS_PENDING, List.of("st-1", "st-2"));
        SubTask st4 = createSubTask("st-4", "Task D", SubTask.STATUS_PENDING, List.of("st-3"));
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2, st3, st4)));

        SubTask next = plan.findNextReadySubTask();

        assertNotNull(next);
        assertEquals("st-3", next.getId());
    }

    @Test
    void isAllCompletedShouldReturnTrueWhenAllCompleted() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_COMPLETED, List.of());
        SubTask st2 = createSubTask("st-2", "Task B", SubTask.STATUS_COMPLETED, List.of());
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        assertTrue(plan.isAllCompleted());
    }

    @Test
    void isAllCompletedShouldReturnFalseWhenSomePending() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_COMPLETED, List.of());
        SubTask st2 = createSubTask("st-2", "Task B", SubTask.STATUS_PENDING, List.of());
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        assertFalse(plan.isAllCompleted());
    }

    @Test
    void isAllCompletedShouldReturnFalseForEmptyList() {
        plan.setSubTasks(new ArrayList<>());

        assertFalse(plan.isAllCompleted());
    }

    @Test
    void getSubTaskByIdShouldReturnMatchingTask() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_PENDING, List.of());
        SubTask st2 = createSubTask("st-2", "Task B", SubTask.STATUS_PENDING, List.of());
        plan.setSubTasks(new ArrayList<>(List.of(st1, st2)));

        SubTask found = plan.getSubTaskById("st-2");

        assertNotNull(found);
        assertEquals("Task B", found.getDescription());
    }

    @Test
    void getSubTaskByIdShouldReturnNullForMissingId() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_PENDING, List.of());
        plan.setSubTasks(new ArrayList<>(List.of(st1)));

        assertNull(plan.getSubTaskById("nonexistent"));
    }

    @Test
    void getSubTaskByIdShouldReturnNullForNullId() {
        SubTask st1 = createSubTask("st-1", "Task A", SubTask.STATUS_PENDING, List.of());
        plan.setSubTasks(new ArrayList<>(List.of(st1)));

        assertNull(plan.getSubTaskById(null));
    }

    @Test
    void getSubTasksShouldInitializeEmptyListWhenNull() {
        plan.setSubTasks(null);

        List<SubTask> tasks = plan.getSubTasks();

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    private SubTask createSubTask(String id, String description, String status, List<String> deps) {
        SubTask st = new SubTask();
        st.setId(id);
        st.setDescription(description);
        st.setStatus(status);
        st.setDependencies(new ArrayList<>(deps));
        return st;
    }
}
