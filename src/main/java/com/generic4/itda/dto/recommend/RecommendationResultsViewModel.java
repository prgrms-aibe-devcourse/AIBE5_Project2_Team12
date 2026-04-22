package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationResultsViewModel(
        Long proposalId,
        Long runId,
        String proposalTitle,
        String positionTitle,
        int topK,
        Integer candidateCount,
        List<RecommendationCandidateItem> candidates
) {}
