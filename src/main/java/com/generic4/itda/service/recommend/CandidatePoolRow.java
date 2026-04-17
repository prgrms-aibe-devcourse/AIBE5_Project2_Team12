package com.generic4.itda.service.recommend;

public record CandidatePoolRow(
        Long resumeId,
        long matchedRequiredSkillCount,
        int weightedProficiencySum,
        byte careerYears
) {

}
