package com.generic4.itda.service.recommend.scoring.model;

public record ScoredCandidate(
        RecommendationScorableCandidate candidate,
        ScoreBreakdown scoreBreakdown
) {

}
