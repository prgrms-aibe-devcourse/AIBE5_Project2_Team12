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
        boolean llmReady,
        /**
         * 현재 매칭 상태. null이면 매칭 요청이 없는 상태 (버튼 활성).
         * 값이 있으면 MatchingStatus.name() 문자열 (예: "PROPOSED", "ACCEPTED", ...).
         */
        String matchingStatus
) {}
