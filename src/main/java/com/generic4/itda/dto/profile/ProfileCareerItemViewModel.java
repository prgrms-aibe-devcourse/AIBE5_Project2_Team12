package com.generic4.itda.dto.profile;

import java.util.List;

public record ProfileCareerItemViewModel(
        String companyName,
        String position,
        String employmentTypeLabel,
        String periodLabel,
        String summary,
        List<String> techStack
) {
    public ProfileCareerItemViewModel {
        techStack = techStack != null ? List.copyOf(techStack) : List.of();
    }
}
