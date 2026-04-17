package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationEntryPositionItem(
        Long proposalPositionId,
        String positionName,
        Long headCount,
        String budgetText,
        List<RecommendationEntrySkillItem> skills
) {

}
