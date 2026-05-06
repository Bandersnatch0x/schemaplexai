package com.schemaplexai.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseMapperXTest {

    @Test
    void extendsBaseMapper() {
        assertThat(BaseMapper.class.isAssignableFrom(BaseMapperX.class)).isTrue();
    }

    @Test
    void isInterface() {
        assertThat(BaseMapperX.class.isInterface()).isTrue();
    }
}
