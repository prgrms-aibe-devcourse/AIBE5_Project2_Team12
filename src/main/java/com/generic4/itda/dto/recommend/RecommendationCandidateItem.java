package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationCandidateItem(
        Long resultId,
        int rank,
        String maskedName,
        String introduction,
        int careerYears,
        String preferredWorkTypeLabel,
        int finalScorePercent,
        int embeddingScorePercent,
        List<String> matchedSkills,
        List<String> highlights,
        String llmReason,
        String llmStatusLabel,
        boolean llmReady
) {}
