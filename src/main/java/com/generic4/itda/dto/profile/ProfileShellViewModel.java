package com.generic4.itda.dto.profile;

public record ProfileShellViewModel(
        ProfileSubjectType subjectType,
        ProfileContextType contextType,
        ProfileAccessLevel accessLevel,
        String title,
        String subtitle,
        String statusLabel,
        String backUrl,
        ProfileFreelancerBodyViewModel freelancer,
        ProfileClientBodyViewModel client,
        ProfileMatchingContextViewModel matchingContext,
        ProfileRecommendationContextViewModel recommendationContext
) {
    public boolean freelancerSubject() {
        return subjectType == ProfileSubjectType.FREELANCER;
    }

    public boolean clientSubject() {
        return subjectType == ProfileSubjectType.CLIENT;
    }

    public boolean hasRecommendationContext() {
        return contextType == ProfileContextType.RECOMMENDATION && recommendationContext != null;
    }

    public boolean hasMatchingContext() {
        return contextType == ProfileContextType.MATCHING && matchingContext != null;
    }
}
