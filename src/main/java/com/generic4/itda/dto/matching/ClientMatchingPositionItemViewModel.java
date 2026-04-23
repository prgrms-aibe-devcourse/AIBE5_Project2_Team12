package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.proposal.ProposalPositionStatus;

public record ClientMatchingPositionItemViewModel(
        Long proposalPositionId,
        String title,
        ProposalPositionStatus status,
        String statusLabel,
        long requestedCount,
        long activeCount
) {
}

