package com.schemaplexai.agent.engine.goal;

/**
 * Immutable record tracking whether a single milestone has been achieved.
 *
 * @param milestone milestone description
 * @param achieved  whether the milestone is considered complete
 * @param evidence  textual evidence supporting the achievement status
 */
public record MilestoneStatus(
        String milestone,
        boolean achieved,
        String evidence
) {

    public static MilestoneStatus pending(String milestone) {
        return new MilestoneStatus(milestone, false, null);
    }

    public static MilestoneStatus achieved(String milestone, String evidence) {
        return new MilestoneStatus(milestone, true, evidence);
    }

    public static MilestoneStatus notAchieved(String milestone, String evidence) {
        return new MilestoneStatus(milestone, false, evidence);
    }
}
