package com.generic4.itda.dto.recommend;

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
) {}

