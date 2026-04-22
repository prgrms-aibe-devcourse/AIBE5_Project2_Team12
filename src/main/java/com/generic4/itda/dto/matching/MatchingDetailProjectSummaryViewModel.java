package com.generic4.itda.dto.matching;

import java.util.List;

public record MatchingDetailProjectSummaryViewModel(
        Long proposalId,
        String proposalTitle,
        String description,
        String positionTitle,
        String positionCategory,
        String budgetText,
        String expectedPeriodText,
        String workTypeLabel,
        List<String> essentialSkills,
        List<String> preferredSkills
) {
}
