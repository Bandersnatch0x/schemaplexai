package com.schemaplexai.common.page;

import lombok.Data;

@Data
public class PageParam {

    private Long current = 1L;
    private Long size = 10L;

    public Long getOffset() {
        return (current - 1) * size;
    }
}
