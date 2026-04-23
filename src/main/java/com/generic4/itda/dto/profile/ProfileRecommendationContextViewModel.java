package com.generic4.itda.dto.profile;

import java.util.List;

public record ProfileRecommendationContextViewModel(
        Long proposalId,
        Long runId,
        Long resultId,
        Long matchingId,
        String matchingStatus,
        int rank,
        int finalScorePercent,
        int embeddingScorePercent,
        List<String> matchedSkills,
        List<String> highlights,
        String llmReason,
        String llmStatusLabel,
        boolean llmReady,
        String requestRedirectUrl,
        String requestErrorRedirectUrl,
        String matchingDetailUrl
) {
    public ProfileRecommendationContextViewModel {
        matchedSkills = matchedSkills != null ? List.copyOf(matchedSkills) : List.of();
        highlights = highlights != null ? List.copyOf(highlights) : List.of();
    }

    public boolean requestable() {
        return matchingStatus == null;
    }

    public boolean proposed() {
        return "PROPOSED".equals(matchingStatus);
    }

    public boolean active() {
        return "ACCEPTED".equals(matchingStatus) || "IN_PROGRESS".equals(matchingStatus);
    }

    public boolean rejected() {
        return "REJECTED".equals(matchingStatus);
    }

    public boolean closed() {
        return "COMPLETED".equals(matchingStatus) || "CANCELED".equals(matchingStatus);
    }

    public boolean hasMatchingDetail() {
        return matchingId != null && matchingDetailUrl != null;
    }
}
