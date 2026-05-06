package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.entity.SfSpecSteering;

import java.util.List;
import java.util.Map;

public interface SpecSteeringService extends IService<SfSpecSteering> {

    /**
     * Evaluate steering rules against input content and return match results.
     *
     * @param specId  the spec id
     * @param content the content to evaluate
     * @return map of rule field to evaluation result
     */
    Map<String, Boolean> evaluateSteeringRules(Long specId, String content);

    /**
     * Apply steering configuration to content and return guided result.
     *
     * @param specId  the spec id
     * @param content the original content
     * @return the guided content after applying steering rules
     */
    String applySteering(Long specId, String content);

    /**
     * List all active steerings for a spec.
     *
     * @param specId the spec id
     * @return list of active steerings
     */
    List<SfSpecSteering> listActiveSteerings(Long specId);

    /**
     * Validate steering configuration for completeness.
     *
     * @param steeringId the steering id
     * @return true if valid
     */
    boolean validateSteeringConfig(Long steeringId);
}
