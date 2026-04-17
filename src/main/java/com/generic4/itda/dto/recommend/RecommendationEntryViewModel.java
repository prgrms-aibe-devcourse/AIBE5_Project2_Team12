package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationEntryViewModel(
        Long proposalId,
        String proposalTitle,
        String proposalStatus,
        boolean runnable,
        String helperMessage,
        Long selectedProposalPositionId,
        List<RecommendationEntryPositionItem> positions
) {

}
