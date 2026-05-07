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

    @Test
    void offsetWithCurrentOne_returnsZero() {
        PageParam param = new PageParam();
        param.setCurrent(1L);
        param.setSize(10L);
        assertEquals(0, param.getOffset());
    }

    @Test
    void gettersAndSetters_workCorrectly() {
        PageParam param = new PageParam();
        param.setCurrent(5L);
        param.setSize(50L);
        assertEquals(5L, param.getCurrent());
        assertEquals(50L, param.getSize());
    }
}
