package com.generic4.itda.domain.recommendation.vo;

import java.util.List;

public record ReasonFacts(
        List<String> matchedSkills,
        List<String> matchedDomains,
        Integer careerYears,
        List<String> highlights
) {

}
