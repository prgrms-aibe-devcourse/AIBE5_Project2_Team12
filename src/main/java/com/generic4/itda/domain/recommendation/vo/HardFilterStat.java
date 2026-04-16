package com.generic4.itda.domain.recommendation.vo;

public record HardFilterStat(
        int totalCandidates,
        int afterActiveFilter,
        int afterVisibilityFilter,
        int afterAiEnabledFilter
) {

    public int finalCount() {
        return afterAiEnabledFilter;
    }
}
