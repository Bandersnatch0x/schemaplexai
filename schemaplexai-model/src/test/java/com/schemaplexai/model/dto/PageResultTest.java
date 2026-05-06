package com.schemaplexai.model.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void empty_returnsDefaultValues() {
        PageResult<String> result = PageResult.empty();
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getPages()).isEqualTo(0L);
    }

    @Test
    void of_setsAllFields() {
        List<String> records = Arrays.asList("a", "b", "c");
        PageResult<String> result = PageResult.of(records, 3L, 1L, 10L);

        assertThat(result.getRecords()).containsExactly("a", "b", "c");
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
    }

    @Test
    void of_calculatesPages_correctly() {
        List<String> records = Arrays.asList("a", "b", "c");
        PageResult<String> result = PageResult.of(records, 25L, 1L, 10L);
        assertThat(result.getPages()).isEqualTo(3L);
    }

    @Test
    void of_calculatesPages_exactDivision() {
        List<String> records = Arrays.asList("a", "b", "c", "d", "e");
        PageResult<String> result = PageResult.of(records, 20L, 1L, 10L);
        assertThat(result.getPages()).isEqualTo(2L);
    }

    @Test
    void of_calculatesPages_singlePage() {
        List<String> records = Collections.singletonList("a");
        PageResult<String> result = PageResult.of(records, 1L, 1L, 10L);
        assertThat(result.getPages()).isEqualTo(1L);
    }

    @Test
    void of_withEmptyList() {
        PageResult<String> result = PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    void implementsSerializable() {
        assertThat(java.io.Serializable.class.isAssignableFrom(PageResult.class)).isTrue();
    }
}
