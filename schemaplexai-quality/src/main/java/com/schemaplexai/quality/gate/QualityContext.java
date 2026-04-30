package com.schemaplexai.quality.gate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityContext {

    private Long executionId;
    private Long specId;
    private Map<String, Object> metadata;
}
