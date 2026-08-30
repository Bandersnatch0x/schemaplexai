package com.schemaplexai.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Page envelope of {@code GET /task/jobs}: frontend contract {@code {list, total}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<JobRecordVO> list;
    private long total;
}
