package com.generic4.itda.service.recommend.scoring.model;

import java.util.Set;

public record ScoredCandidate(
        RecommendationScorableCandidate candidate,
        ScoreBreakdown scoreBreakdown
) {
    
    public Long resumeId() {
        return candidate.resumeId();
    }

    public int careerYears() {
        return candidate.careerYears();
    }

    public Set<String> ownedSkillNames() {
        return candidate.ownedSkillNames();
    }

    public double similarityScore() {
        return scoreBreakdown.similarityScore();
    }

    public double skillAdjustmentScore() {
        return scoreBreakdown.skillAdjustmentScore();
    }

    public double careerAdjustmentScore() {
        return scoreBreakdown.careerAdjustmentScore();
    }

    public double finalScore() {
        return scoreBreakdown.finalScore();
    }
}
