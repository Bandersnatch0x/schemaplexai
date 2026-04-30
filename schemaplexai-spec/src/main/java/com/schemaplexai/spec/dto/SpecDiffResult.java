package com.schemaplexai.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecDiffResult {

    private Long specId;
    private Long versionAId;
    private Long versionBId;
    private List<DiffHunk> hunks;
}
