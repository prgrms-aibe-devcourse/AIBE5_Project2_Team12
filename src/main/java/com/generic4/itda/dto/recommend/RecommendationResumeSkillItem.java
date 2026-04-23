package com.generic4.itda.dto.recommend;

public record RecommendationResumeSkillItem(
        String name,
        String proficiencyLabel,
        String proficiencyCode
) {
    public RecommendationResumeSkillItem(String name, String proficiencyLabel) {
        this(name, proficiencyLabel, null);
    }
}
