package com.generic4.itda.dto.recommend;

import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import java.time.LocalDateTime;

public record RecommendationRunHistoryItemViewModel(
        Long runId,
        RecommendationRunStatus status,
        String statusLabel,
        Integer candidateCount,
        int topK,
        LocalDateTime requestedAt,
        LocalDateTime updatedAt
) {
    public boolean completed() {
        return status == RecommendationRunStatus.COMPUTED;
    }

    public boolean processing() {
        return status == RecommendationRunStatus.PENDING || status == RecommendationRunStatus.RUNNING;
    }
}
