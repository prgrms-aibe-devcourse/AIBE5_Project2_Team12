package com.generic4.itda.service.recommend.scoring.model;

import java.util.Set;

public record RecommendationScorableCandidate(
        Long candidateId,
        Long memberId,
        Long resumeId,
        int careerYears,
        Set<String> ownedSkillNames
) {

}
