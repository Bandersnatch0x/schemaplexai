package com.schemaplexai.common.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageParamTest {

    @Test
    void defaultValues() {
        PageParam param = new PageParam();
        assertEquals(1, param.getCurrent());
        assertEquals(10, param.getSize());
    }

    @Test
    void offsetCalculation() {
        PageParam param = new PageParam();
        param.setCurrent(3L);
        param.setSize(20L);
        assertEquals(40, param.getOffset());
    }
}
