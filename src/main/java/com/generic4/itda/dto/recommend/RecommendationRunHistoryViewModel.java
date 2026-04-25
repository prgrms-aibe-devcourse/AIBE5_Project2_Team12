package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationRunHistoryViewModel(
        Long proposalId,
        Long proposalPositionId,
        String proposalTitle,
        String positionTitle,
        String positionStatusName,
        String positionStatusLabel,
        boolean runnable,
        String helperMessage,
        List<RecommendationRunHistoryItemViewModel> runs
) {
    public boolean hasRuns() {
        return runs != null && !runs.isEmpty();
    }
}
