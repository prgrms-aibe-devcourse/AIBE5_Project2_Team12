package com.generic4.itda.service.recommend.scoring.model;

public record ScoreBreakdown(
        double similarityScore,
        double skillAdjustmentScore,
        double careerAdjustmentScore,
        double finalScore
) {

}
