package com.schemaplexai.agent.engine.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single sub-task within a {@link SubTaskPlan}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTask {

    private String id;
    private String description;
    private String status;
    private List<String> dependencies;
    private String result;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public List<String> getDependencies() {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        return dependencies;
    }
}
