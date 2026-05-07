package com.schemaplexai.agent.engine.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a decomposed plan of sub-tasks for achieving a goal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubTaskPlan {

    private String goal;
    private List<SubTask> subTasks;
    private String currentSubTaskId;

    public List<SubTask> getSubTasks() {
        if (subTasks == null) {
            subTasks = new ArrayList<>();
        }
        return subTasks;
    }

    /**
     * Finds the next sub-task that is PENDING and has all dependencies satisfied.
     *
     * @return the next ready sub-task, or null if none available
     */
    @JsonIgnore
    public SubTask findNextReadySubTask() {
        Set<String> completedIds = getSubTasks().stream()
                .filter(t -> SubTask.STATUS_COMPLETED.equals(t.getStatus()))
                .map(SubTask::getId)
                .collect(Collectors.toSet());

        for (SubTask subTask : getSubTasks()) {
            if (!SubTask.STATUS_PENDING.equals(subTask.getStatus())) {
                continue;
            }
            boolean depsSatisfied = subTask.getDependencies().stream()
                    .allMatch(completedIds::contains);
            if (depsSatisfied) {
                return subTask;
            }
        }
        return null;
    }

    /**
     * Checks whether all sub-tasks are completed.
     *
     * @return true if every sub-task status is COMPLETED
     */
    @JsonIgnore
    public boolean isAllCompleted() {
        if (getSubTasks().isEmpty()) {
            return false;
        }
        return getSubTasks().stream()
                .allMatch(t -> SubTask.STATUS_COMPLETED.equals(t.getStatus()));
    }

    /**
     * Retrieves a sub-task by its ID.
     *
     * @param id the sub-task ID
     * @return the matching sub-task, or null if not found
     */
    @JsonIgnore
    public SubTask getSubTaskById(String id) {
        if (id == null) {
            return null;
        }
        return getSubTasks().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .orElse(null);
    }
}
