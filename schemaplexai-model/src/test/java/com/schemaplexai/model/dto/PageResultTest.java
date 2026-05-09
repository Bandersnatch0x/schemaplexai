package com.schemaplexai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult")
class PageResultTest {

    @Test
    @DisplayName("should create empty PageResult")
    void shouldCreateEmpty() {
        PageResult<String> result = PageResult.empty();

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getPages()).isEqualTo(0L);
    }

    @Test
    @DisplayName("should create PageResult with records via of()")
    void shouldCreateWithRecords() {
        List<String> records = List.of("a", "b", "c");
        PageResult<String> result = PageResult.of(records, 10L, 1L, 5L);

        assertThat(result.getRecords()).containsExactly("a", "b", "c");
        assertThat(result.getTotal()).isEqualTo(10L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getPages()).isEqualTo(2L);
    }

    @Test
    @DisplayName("should calculate pages correctly for exact division")
    void shouldCalculatePagesForExactDivision() {
        PageResult<String> result = PageResult.of(List.of(), 20L, 1L, 5L);
        assertThat(result.getPages()).isEqualTo(4L);
    }

    @Test
    @DisplayName("should calculate pages correctly for remainder")
    void shouldCalculatePagesForRemainder() {
        PageResult<String> result = PageResult.of(List.of(), 22L, 1L, 5L);
        assertThat(result.getPages()).isEqualTo(5L);
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        PageResult<String> result = new PageResult<>();
        result.setRecords(List.of("x"));
        result.setTotal(100L);
        result.setCurrent(2L);
        result.setSize(20L);
        result.setPages(5L);

        assertThat(result.getRecords()).containsExactly("x");
        assertThat(result.getTotal()).isEqualTo(100L);
        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(20L);
        assertThat(result.getPages()).isEqualTo(5L);
    }
}
