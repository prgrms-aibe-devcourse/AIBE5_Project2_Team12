package com.generic4.itda.dto.profile;

import java.util.List;

public record ProfileFreelancerBodyViewModel(
        String displayName,
        String headline,
        String introduction,
        Integer careerYears,
        String preferredWorkTypeLabel,
        String portfolioUrl,
        List<ProfileSkillItemViewModel> skills,
        List<ProfileCareerItemViewModel> careerItems
) {
    public ProfileFreelancerBodyViewModel {
        skills = skills != null ? List.copyOf(skills) : List.of();
        careerItems = careerItems != null ? List.copyOf(careerItems) : List.of();
    }
}
