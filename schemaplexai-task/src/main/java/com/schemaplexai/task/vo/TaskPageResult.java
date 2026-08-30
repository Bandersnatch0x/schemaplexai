package com.schemaplexai.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Page envelope of {@code GET /task/tasks}. The frontend contract expects
 * exactly {@code {list, total}} (not the repository-default {@code records}),
 * so this dedicated shape is used instead of the shared {@code PageResult}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<TaskVO> list;
    private long total;
}
