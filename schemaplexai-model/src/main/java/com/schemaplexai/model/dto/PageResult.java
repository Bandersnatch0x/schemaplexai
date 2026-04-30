package com.schemaplexai.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();
    private Long total = 0L;
    private Long current = 1L;
    private Long size = 10L;
    private Long pages = 0L;

    public static <T> PageResult<T> of(List<T> records, Long total, Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages((total + size - 1) / size);
        return result;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
