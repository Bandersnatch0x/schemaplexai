package com.schemaplexai.web.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * M6.4: Paginated execution list wrapper for HTTP API responses.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPageVO extends ExecutionStatusVO {

    private long total;

    private long page;

    private long size;

    private List<ExecutionStatusVO> records;
}
