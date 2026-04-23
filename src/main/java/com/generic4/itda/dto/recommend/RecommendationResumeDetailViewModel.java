package com.generic4.itda.dto.recommend;

import com.generic4.itda.dto.profile.ProfileAccessLevel;
import com.generic4.itda.dto.profile.ProfileCareerItemViewModel;
import com.generic4.itda.dto.profile.ProfileContextType;
import com.generic4.itda.dto.profile.ProfileFreelancerBodyViewModel;
import com.generic4.itda.dto.profile.ProfileRecommendationContextViewModel;
import com.generic4.itda.dto.profile.ProfileShellViewModel;
import com.generic4.itda.dto.profile.ProfileSkillItemViewModel;
import com.generic4.itda.dto.profile.ProfileSubjectType;
import java.util.List;

public record RecommendationResumeDetailViewModel(
        Long proposalId,
        Long runId,
        Long resultId,
        String proposalTitle,
        String positionTitle,
        String backUrl,
        RecommendationCandidateItem candidate,
        String portfolioUrl,
        List<RecommendationResumeSkillItem> resumeSkills,
        List<RecommendationResumeCareerItem> careerItems
) {
    public ProfileShellViewModel profile() {
        List<RecommendationResumeSkillItem> safeSkills = resumeSkills != null ? resumeSkills : List.of();
        List<RecommendationResumeCareerItem> safeCareers = careerItems != null ? careerItems : List.of();

        return new ProfileShellViewModel(
                ProfileSubjectType.FREELANCER,
                ProfileContextType.RECOMMENDATION,
                ProfileAccessLevel.PREVIEW,
                "추천 후보 프로필",
                proposalTitle + " · " + positionTitle,
                "추천 미리보기",
                backUrl,
                new ProfileFreelancerBodyViewModel(
                        candidate.maskedName(),
                        "프리랜서 프로필",
                        candidate.introduction(),
                        candidate.careerYears(),
                        candidate.preferredWorkTypeLabel(),
                        portfolioUrl,
                        safeSkills.stream()
                                .map(skill -> new ProfileSkillItemViewModel(
                                        skill.name(),
                                        skill.proficiencyLabel(),
                                        skill.proficiencyCode()
                                ))
                                .toList(),
                        safeCareers.stream()
                                .map(career -> new ProfileCareerItemViewModel(
                                        career.companyName(),
                                        career.position(),
                                        career.employmentTypeLabel(),
                                        career.periodLabel(),
                                        career.summary(),
                                        career.techStack()
                                ))
                                .toList()
                ),
                null,
                null,
                new ProfileRecommendationContextViewModel(
                        proposalId,
                        runId,
                        resultId,
                        candidate.matchingId(),
                        candidate.matchingStatus(),
                        candidate.rank(),
                        candidate.finalScorePercent(),
                        candidate.embeddingScorePercent(),
                        candidate.matchedSkills(),
                        candidate.highlights(),
                        candidate.llmReason(),
                        candidate.llmStatusLabel(),
                        candidate.llmReady(),
                        "/proposals/%d/recommendations/results/%d".formatted(proposalId, resultId),
                        "/proposals/%d/recommendations/results/%d".formatted(proposalId, resultId),
                        candidate.matchingId() != null ? "/matchings/%d".formatted(candidate.matchingId()) : null
                )
        );
    }
}
