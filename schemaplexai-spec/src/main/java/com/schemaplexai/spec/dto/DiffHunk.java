package com.schemaplexai.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffHunk {

    private int oldStart;
    private int oldLines;
    private int newStart;
    private int newLines;
    private List<LineChange> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineChange {
        private String type; // ADDED, REMOVED, UNCHANGED
        private String content;
    }
}
