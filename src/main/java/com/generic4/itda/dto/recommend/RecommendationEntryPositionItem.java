package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationEntryPositionItem(
        Long proposalPositionId,
        String positionTitle,
        String positionCategoryName,
        Long headCount,
        String budgetText,
        Long expectedPeriod,
        List<RecommendationEntrySkillItem> skills
) {

}
