package com.generic4.itda.dto.profile;

import java.util.List;

public record ProfileProjectSummaryViewModel(
        Long proposalId,
        String proposalTitle,
        String description,
        String positionTitle,
        String workTypeLabel,
        String budgetText,
        String expectedPeriodText,
        List<String> essentialSkills,
        List<String> preferredSkills
) {
    public ProfileProjectSummaryViewModel {
        essentialSkills = essentialSkills != null ? List.copyOf(essentialSkills) : List.of();
        preferredSkills = preferredSkills != null ? List.copyOf(preferredSkills) : List.of();
    }
}
